package com.mushroom.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mushroom.entity.OrderDetail;
import com.mushroom.entity.Orders;
import com.mushroom.mapper.ProductMapper;
import com.mushroom.service.OrderDetailService;
import com.mushroom.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时未支付订单回收
 *
 * 为什么需要：
 *   下单时就会扣减库存（属于"下单锁库存"的正常做法），但原实现没有回滚机制 ——
 *   用户下了单不付款，商品库存就被永久占着，别人买不到，最后变成"明明有货却显示售罄"。
 *   前端支付页有 900 秒倒计时，但倒计时结束只是提示，不会真的取消订单。
 *
 * 做法：
 *   每 2 分钟扫一次「待付款且创建超过 15 分钟」的订单，逐单处理：
 *     1) 用条件更新把状态从 1 改成 5，只有改成功的那一次才会继续（防止用户此刻刚好付款被误杀）
 *     2) 原子恢复库存与销量
 *   单笔处理放在独立事务里，一笔失败不影响其他订单。
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    /** 支付超时分钟数，与前端的 900 秒倒计时保持一致 */
    private static final int TIMEOUT_MINUTES = 15;

    /** 单轮最多处理多少笔，避免积压时一次性全表扫描 */
    private static final int BATCH_SIZE = 200;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** 启动后延迟 1 分钟开始，之后每 2 分钟一次 */
    @Scheduled(initialDelay = 60_000, fixedDelay = 120_000)
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getStatus, 1);
        wrapper.lt(Orders::getCreateTime, deadline);
        wrapper.orderByAsc(Orders::getId);
        wrapper.last("LIMIT " + BATCH_SIZE);

        List<Orders> timeoutOrders = ordersService.list(wrapper);
        if (timeoutOrders.isEmpty()) {
            return;
        }

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        int cancelled = 0;
        for (Orders orders : timeoutOrders) {
            try {
                Boolean done = tx.execute(status -> cancelOne(orders.getId()));
                if (Boolean.TRUE.equals(done)) {
                    cancelled++;
                }
            } catch (Exception e) {
                log.warn("超时订单回收失败: orderId={}", orders.getId(), e);
            }
        }
        log.info("超时未支付订单回收：扫描 {} 笔，实际取消 {} 笔", timeoutOrders.size(), cancelled);
    }

    /**
     * 取消单笔超时订单并恢复库存
     * @return true 表示本次确实取消了（状态由 1 变为 5）；false 说明状态已被其他操作改变
     */
    private Boolean cancelOne(Long orderId) {
        boolean updated = ordersService.update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, 1)
                .set(Orders::getStatus, 5));
        if (!updated) {
            // 扫描之后、更新之前用户刚好付款了，保持原样
            return false;
        }

        LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(OrderDetail::getOrderId, orderId);
        for (OrderDetail detail : orderDetailService.list(detailWrapper)) {
            productMapper.restoreStockAndReduceSale(detail.getProductId(), detail.getNumber());
        }
        return true;
    }
}

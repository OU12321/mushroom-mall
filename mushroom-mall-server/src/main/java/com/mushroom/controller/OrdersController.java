package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.OrderDetail;
import com.mushroom.entity.Orders;
import com.mushroom.entity.Product;
import com.mushroom.mapper.ProductMapper;
import com.mushroom.service.OrderDetailService;
import com.mushroom.service.OrdersService;
import com.mushroom.service.ProductService;
import com.mushroom.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/orders")
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ReviewService reviewService;

    /**
     * 分页查询订单
     * 管理员看全部，商家看自家(merchantId)，用户看自己(userId)
     */
    @GetMapping("/page")
    public Result<Page<Orders>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            HttpServletRequest request) {

        Integer role = (Integer) request.getAttribute("role");
        Long userId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Orders::getStatus, status);
        String trimmedOrderNo = (orderNo == null) ? null : orderNo.trim();
        wrapper.like(trimmedOrderNo != null && !trimmedOrderNo.isEmpty(), Orders::getOrderNo, trimmedOrderNo);

        if (role == 0) {
            // 管理员看全部
        } else if (role == 1) {
            // 商家看自家订单
            wrapper.eq(Orders::getMerchantId, userId);
        } else {
            // 用户看自己订单
            wrapper.eq(Orders::getUserId, userId);
        }

        wrapper.orderByDesc(Orders::getCreateTime);
        Page<Orders> pageInfo = ordersService.page(new Page<>(page, pageSize), wrapper);

        // 填充订单明细
        List<Orders> records = pageInfo.getRecords();
        for (Orders orders : records) {
            LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(OrderDetail::getOrderId, orders.getId());
            orders.setOrderDetails(orderDetailService.list(detailWrapper));
        }

        // 标记用户是否已评价（当前只判断订单的第一个商品）
        if (role != null && role == 2 && userId != null && !records.isEmpty()) {
            LambdaQueryWrapper<com.mushroom.entity.Review> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.eq(com.mushroom.entity.Review::getUserId, userId);
            List<Long> orderIds = records.stream().map(Orders::getId).collect(java.util.stream.Collectors.toList());
            reviewWrapper.in(com.mushroom.entity.Review::getOrderId, orderIds);
            List<com.mushroom.entity.Review> reviews = reviewService.list(reviewWrapper);
            Map<Long, List<Long>> reviewedMap = new HashMap<>();
            for (com.mushroom.entity.Review r : reviews) {
                reviewedMap.computeIfAbsent(r.getOrderId(), k -> new java.util.ArrayList<>()).add(r.getProductId());
            }
            for (Orders orders : records) {
                List<OrderDetail> details = orders.getOrderDetails();
                if (details != null && !details.isEmpty()) {
                    Long firstProductId = details.get(0).getProductId();
                    List<Long> productIds = reviewedMap.get(orders.getId());
                    orders.setReviewed(productIds != null && productIds.contains(firstProductId));
                }
            }
        }

        return Result.success(pageInfo);
    }

    /**
     * 订单详情 (含order_detail列表)
     *
     * 归属校验：只有下单用户本人、订单所属商家、管理员可以查看。
     * 否则可遍历 id 读取他人订单中的收货人、电话、地址等隐私信息。
     */
    @GetMapping("/{id}")
    public Result<Orders> getById(@PathVariable Long id, HttpServletRequest request) {
        Orders orders = ordersService.getById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        boolean isAdmin = role != null && role == 0;
        boolean isOwner = userId != null && userId.equals(orders.getUserId());
        boolean isOrderMerchant = userId != null && userId.equals(orders.getMerchantId());
        if (!isAdmin && !isOwner && !isOrderMerchant) {
            throw new BusinessException("无权查看该订单");
        }
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> details = orderDetailService.list(wrapper);
        orders.setOrderDetails(details);
        return Result.success(orders);
    }

    /**
     * 用户下单
     */
    @PostMapping
    @Transactional
    public Result<Long> placeOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        String consignee = (String) body.get("consignee");
        String phone = (String) body.get("phone");
        String address = (String) body.get("address");
        Integer payMethod = (Integer) body.get("payMethod");
        String remark = (String) body.get("remark");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

        if (consignee == null || consignee.trim().isEmpty() ||
            phone == null || phone.trim().isEmpty() ||
            address == null || address.trim().isEmpty() ||
            items == null || items.isEmpty()) {
            throw new BusinessException("订单信息不完整");
        }

        // 生成订单号：DD + 毫秒时间戳 + 6位随机数
        // 原实现随机部分只有4位，同一毫秒内多个用户下单存在碰撞到 uk_order_no 唯一索引的可能
        String orderNo = "DD" + System.currentTimeMillis()
                + String.format("%06d", (int) (Math.random() * 1000000));

        // 先校验商品、计算金额、确定商家，避免订单主表插入时缺少 merchant_id
        BigDecimal totalAmount = BigDecimal.ZERO;
        Long merchantId = null;
        Map<Long, Product> productMap = new HashMap<>();
        for (Map<String, Object> item : items) {
            Object pidObj = item.get("productId");
            Object numObj = item.get("number");
            if (pidObj == null || numObj == null) {
                throw new BusinessException("订单商品信息不完整");
            }
            Long productId = Long.valueOf(pidObj.toString());
            Integer number = Integer.valueOf(numObj.toString());
            if (number <= 0) {
                throw new BusinessException("商品数量必须大于0");
            }

            Product product = productService.getById(productId);
            if (product == null) {
                throw new BusinessException("商品不存在: " + productId);
            }
            // 只有「审核通过 + 已上架」的商品才能下单。
            // 原实现只校验商品存在与库存，已下架或待审核的商品只要知道 id 仍然能买。
            if (product.getAuditStatus() == null || product.getAuditStatus() != 1
                    || product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException("商品「" + product.getName() + "」已下架");
            }
            if (product.getPrice() == null) {
                throw new BusinessException("商品价格异常: " + product.getName());
            }
            if (product.getMerchantId() == null) {
                throw new BusinessException("商品商家信息缺失: " + product.getName());
            }

            // 库存校验
            int currentStock = product.getStock() == null ? 0 : product.getStock();
            if (currentStock <= 0) {
                throw new BusinessException("商品「" + product.getName() + "」已售罄");
            }
            if (currentStock < number) {
                throw new BusinessException("商品「" + product.getName() + "」库存不足，当前库存: " + currentStock);
            }

            productMap.put(productId, product);
            // 一笔订单只能归属一个商家。
            // 购物车若混选多个商家的商品，原实现只取第一个商品的商家，
            // 会导致另一个商家的货"白卖"且看不到订单，这里直接明确报错。
            if (merchantId == null) {
                merchantId = product.getMerchantId();
            } else if (!merchantId.equals(product.getMerchantId())) {
                throw new BusinessException("一笔订单只能结算同一商家的商品，请分开下单");
            }
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(number)));
        }

        // 创建订单主表
        Orders orders = new Orders();
        orders.setOrderNo(orderNo);
        orders.setUserId(userId);
        orders.setMerchantId(merchantId);
        orders.setConsignee(consignee);
        orders.setPhone(phone);
        orders.setAddress(address);
        orders.setPayMethod(payMethod);
        orders.setRemark(remark);
        orders.setStatus(1); // 待付款
        orders.setTotalAmount(totalAmount);
        ordersService.save(orders);

        // 保存订单明细并增加销量
        for (Map<String, Object> item : items) {
            Long productId = Long.valueOf(item.get("productId").toString());
            Integer number = Integer.valueOf(item.get("number").toString());
            Product product = productMap.get(productId);

            BigDecimal itemAmount = product.getPrice().multiply(new BigDecimal(number));

            OrderDetail detail = new OrderDetail();
            detail.setOrderId(orders.getId());
            detail.setProductId(productId);
            detail.setProductName(product.getName());
            detail.setProductImage(product.getImage());
            detail.setPrice(product.getPrice());
            detail.setNumber(number);
            detail.setAmount(itemAmount);
            orderDetailService.save(detail);

            // 扣减库存 + 增加销量
            // 改为数据库原子更新：判断与扣减在同一条 UPDATE 内完成，避免并发下单超卖；
            // 受影响行数为 0 说明库存不足，抛异常由 @Transactional 整体回滚
            int affectedRows = productMapper.deductStockAndAddSale(productId, number);
            if (affectedRows == 0) {
                throw new BusinessException("商品「" + product.getName() + "」库存不足，请调整数量后重试");
            }
        }

        return Result.success("下单成功", orders.getId());
    }

    /**
     * 用户付款
     */
    @PutMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Orders orders = ordersService.getById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        if (!orders.getUserId().equals(userId)) {
            throw new BusinessException("只能支付自己的订单");
        }
        if (orders.getStatus() != 1) {
            throw new BusinessException("订单状态不正确，无法付款");
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // 条件更新：只有库里状态仍是「待付款」才写得进。
        // 原实现是「查出来判断 → 再写回」，两个并发请求会同时通过判断，
        // 造成重复付款（沙盒无害，但状态机本身是错的）。
        boolean ok = ordersService.update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, id)
                .eq(Orders::getStatus, 1)
                .set(Orders::getStatus, 2)
                .set(Orders::getPayTime, now));
        if (!ok) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        return Result.success("付款成功", null);
    }

    /**
     * 商家发货
     */
    @PutMapping("/ship/{id}")
    public Result<Void> ship(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Orders orders = ordersService.getById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        if (!orders.getMerchantId().equals(userId)) {
            log.warn("商家发货权限校验失败：当前用户ID={}, 订单归属商家ID={}, 订单ID={}", userId, orders.getMerchantId(), id);
            throw new BusinessException("只能操作自家订单");
        }
        if (orders.getStatus() != 2) {
            throw new BusinessException("订单状态不正确，无法发货");
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // 条件更新，避免两个标签页同时点「发货」把状态推两次
        boolean ok = ordersService.update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, id)
                .eq(Orders::getStatus, 2)
                .set(Orders::getStatus, 3)
                .set(Orders::getShipTime, now));
        if (!ok) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        return Result.success("发货成功", null);
    }

    /**
     * 用户确认收货
     */
    @PutMapping("/receive/{id}")
    public Result<Void> receive(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Orders orders = ordersService.getById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        if (!orders.getUserId().equals(userId)) {
            throw new BusinessException("只能操作自己的订单");
        }
        if (orders.getStatus() != 3) {
            throw new BusinessException("订单状态不正确，无法确认收货");
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        boolean ok = ordersService.update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, id)
                .eq(Orders::getStatus, 3)
                .set(Orders::getStatus, 4)
                .set(Orders::getReceiveTime, now));
        if (!ok) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        return Result.success("确认收货成功", null);
    }

    /**
     * 用户取消订单
     */
    @PutMapping("/cancel/{id}")
    @Transactional
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Orders orders = ordersService.getById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        if (!orders.getUserId().equals(userId)) {
            throw new BusinessException("只能取消自己的订单");
        }
        if (orders.getStatus() != 1) {
            throw new BusinessException("订单状态不正确，无法取消");
        }

        // 先用条件更新抢占「待付款 → 已取消」，再恢复库存。
        // 这样即使连点两次取消（或两个标签页同时点），也只有一次能把 1 改成 5，
        // 库存只恢复一次；原实现两个请求都会走完恢复逻辑，库存被加两遍。
        boolean ok = ordersService.update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, id)
                .eq(Orders::getStatus, 1)
                .set(Orders::getStatus, 5));
        if (!ok) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }

        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> details = orderDetailService.list(wrapper);
        for (OrderDetail detail : details) {
            // 原子恢复库存与销量（与下单扣减对称），避免并发丢失更新
            productMapper.restoreStockAndReduceSale(detail.getProductId(), detail.getNumber());
        }
        return Result.success("订单已取消", null);
    }

    /**
     * 用户申请退款
     */
    @PutMapping("/refund/{id}")
    public Result<Void> refund(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Orders orders = ordersService.getById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        if (!orders.getUserId().equals(userId)) {
            throw new BusinessException("只能申请自己的订单退款");
        }
        if (orders.getStatus() != 2 && orders.getStatus() != 3 && orders.getStatus() != 4) {
            throw new BusinessException("订单状态不正确，无法申请退款");
        }

        boolean ok = ordersService.update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, id)
                .in(Orders::getStatus, 2, 3, 4)
                .set(Orders::getStatus, 6));
        if (!ok) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        return Result.success("退款申请已提交", null);
    }

    /**
     * 商家/管理员 同意退款
     */
    @PutMapping("/agreeRefund/{id}")
    @Transactional
    public Result<Void> agreeRefund(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        Orders orders = ordersService.getById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        // 管理员可处理全部退款；商家只能处理自家订单。
        // 原写法为 role == 1 时才校验归属，导致 role=0/2 直接跳过校验（普通用户可替他人同意退款）
        boolean isAdmin = role != null && role == 0;
        boolean isOrderMerchant = userId != null && orders.getMerchantId() != null
                && orders.getMerchantId().equals(userId);
        if (!isAdmin && !isOrderMerchant) {
            throw new BusinessException("只能操作自家订单");
        }
        if (orders.getStatus() != 6) {
            throw new BusinessException("订单状态不正确，无法同意退款");
        }
        // 条件更新抢占「退款中 → 已退款」，避免重复点同意导致库存恢复两次
        boolean ok = ordersService.update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, id)
                .eq(Orders::getStatus, 6)
                .set(Orders::getStatus, 7));
        if (!ok) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }

        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> details = orderDetailService.list(wrapper);
        for (OrderDetail detail : details) {
            productMapper.restoreStockAndReduceSale(detail.getProductId(), detail.getNumber());
        }
        return Result.success("已同意退款", null);
    }
}

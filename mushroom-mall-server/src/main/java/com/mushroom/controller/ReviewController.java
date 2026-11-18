package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushroom.annotation.RequireRole;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.OrderDetail;
import com.mushroom.entity.Orders;
import com.mushroom.entity.Product;
import com.mushroom.entity.Review;
import com.mushroom.entity.User;
import com.mushroom.mapper.UserMapper;
import com.mushroom.service.OrderDetailService;
import com.mushroom.service.OrdersService;
import com.mushroom.service.ProductService;
import com.mushroom.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 商品评价控制器
 * 用户评价商品，商家回复，管理员删除违规评价
 */
@Slf4j
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ProductService productService;

    /**
     * 查看商品评价列表
     *
     * 商家端会看到「自家商品」的评价（与 reply 的归属校验保持一致：
     * 否则列表里全是别家商品的评价，点回复会被拒绝）；
     * 管理员与用户端按 productId 正常查询。
     */
    @GetMapping("/page")
    public Result<Page<Review>> page(
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(productId != null, Review::getProductId, productId);

        Integer role = (Integer) request.getAttribute("role");
        Long userId = (Long) request.getAttribute("userId");
        if (role != null && role == 1 && userId != null) {
            // userId 来自 JWT 且为 Long，不存在拼接注入
            wrapper.inSql(Review::getProductId,
                    "SELECT id FROM product WHERE merchant_id = " + userId + " AND is_deleted = 0");
        }

        wrapper.orderByDesc(Review::getCreateTime);

        Page<Review> pageInfo = reviewService.page(new Page<>(page, pageSize), wrapper);
        return Result.success(pageInfo);
    }

    /**
     * 用户评价商品
     *
     * 三层校验：订单存在且属于本人 → 订单已完成 → 该订单中确实包含该商品，
     * 避免对他人订单、未完成订单或订单里没有的商品刷评价。
     * 加事务以缩小"先查重后插入"的并发窗口。
     */
    @Transactional
    @PostMapping
    public Result<Void> save(@RequestBody Review review, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (review.getOrderId() == null || review.getProductId() == null) {
            throw new BusinessException("订单ID和商品ID不能为空");
        }
        if (review.getScore() == null || review.getScore() < 1 || review.getScore() > 5) {
            throw new BusinessException("评分需在1-5之间");
        }

        // 校验订单归属与状态
        Orders order = ordersService.getById(review.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("只能评价自己的订单");
        }
        if (order.getStatus() == null || order.getStatus() != 4) {
            throw new BusinessException("订单完成后才能评价");
        }

        // 校验该订单明细中确实包含该商品
        LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(OrderDetail::getOrderId, review.getOrderId());
        detailWrapper.eq(OrderDetail::getProductId, review.getProductId());
        if (orderDetailService.count(detailWrapper) == 0) {
            throw new BusinessException("该订单中不包含此商品");
        }

        // 校验是否已评价过
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getOrderId, review.getOrderId());
        wrapper.eq(Review::getProductId, review.getProductId());
        wrapper.eq(Review::getUserId, userId);
        long count = reviewService.count(wrapper);
        if (count > 0) {
            throw new BusinessException("该商品已评价过，请勿重复评价");
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);

        review.setUserId(userId);
        review.setUserName(user != null ? user.getName() : "");
        review.setUserAvatar(user != null ? user.getAvatar() : "");
        reviewService.save(review);
        return Result.success("评价成功", null);
    }

    /**
     * 商家回复评价
     *
     * 权限：管理员可回复任意评价；商家只能回复"自家商品"下的评价。
     * 原实现既没有角色校验也没有归属校验，任何登录用户（含普通用户）
     * 都能给任意评价写入"商家回复"，且不校验评价是否存在，
     * updateById 对不存在的 id 会静默成功并返回"回复成功"。
     */
    @PutMapping("/reply/{id}")
    @RequireRole({0, 1})
    public Result<Void> reply(@PathVariable Long id, @RequestBody Review review, HttpServletRequest request) {
        if (review.getReply() == null || review.getReply().trim().isEmpty()) {
            throw new BusinessException("回复内容不能为空");
        }
        String replyText = review.getReply().trim();
        if (replyText.length() > 500) {
            throw new BusinessException("回复内容不能超过500字");
        }

        Review exist = reviewService.getById(id);
        if (exist == null) {
            throw new BusinessException("评价不存在");
        }

        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        boolean isAdmin = role != null && role == 0;

        if (!isAdmin) {
            // 商家：评价所属商品的 merchant_id 必须是自己
            Product product = productService.getById(exist.getProductId());
            if (product == null || product.getMerchantId() == null
                    || !product.getMerchantId().equals(userId)) {
                throw new BusinessException("只能回复自家商品的评价");
            }
        }

        Review update = new Review();
        update.setId(id);
        update.setReply(replyText);
        reviewService.updateById(update);
        return Result.success("回复成功", null);
    }

    /**
     * 管理员删除违规评价
     *
     * 仅管理员可用（原实现无任何校验，任何登录用户都能删掉别人的评价）。
     */
    @DeleteMapping("/{id}")
    @RequireRole(0)
    public Result<Void> delete(@PathVariable Long id) {
        Review exist = reviewService.getById(id);
        if (exist == null) {
            throw new BusinessException("评价不存在");
        }
        reviewService.removeById(id);
        return Result.success("删除成功", null);
    }
}

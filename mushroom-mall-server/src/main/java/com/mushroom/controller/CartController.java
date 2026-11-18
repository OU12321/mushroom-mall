package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.Cart;
import com.mushroom.entity.Product;
import com.mushroom.service.CartService;
import com.mushroom.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 购物车控制器
 */
@Slf4j
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    /**
     * 当前用户购物车列表
     */
    @GetMapping("/list")
    public Result<List<Cart>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.orderByDesc(Cart::getCreateTime);
        List<Cart> list = cartService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 加入购物车
     * 如果该商品已在购物车则数量累加，否则新增
     *
     * 加事务是为了缩小"先查后写"的并发窗口。
     * getOne 传第二个参数 false，表示存在多条时取第一条而不抛 TooManyResultsException，
     * 避免历史脏数据（同商品多行）导致整个购物车打不开。
     */
    @Transactional
    @PostMapping
    public Result<Void> add(@RequestBody Cart cart, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (cart.getProductId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        if (cart.getNumber() == null || cart.getNumber() <= 0) {
            throw new BusinessException("数量必须大于0");
        }

        Product product = productService.getById(cart.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 检查购物车中是否已有该商品
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.eq(Cart::getProductId, cart.getProductId());
        Cart existCart = cartService.getOne(wrapper, false);

        if (existCart != null) {
            // 数量累加
            Cart update = new Cart();
            update.setId(existCart.getId());
            update.setNumber(existCart.getNumber() + cart.getNumber());
            cartService.updateById(update);
            return Result.success("购物车已更新", null);
        }

        // 新增购物车项
        cart.setUserId(userId);
        cart.setMerchantId(product.getMerchantId());
        cart.setProductName(product.getName());
        cart.setProductImage(product.getImage());
        cart.setPrice(product.getPrice());
        cartService.save(cart);
        return Result.success("已加入购物车", null);
    }

    /**
     * 修改购物车商品数量
     */
    @PutMapping
    public Result<Void> update(@RequestBody Cart cart, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (cart.getId() == null) {
            throw new BusinessException("购物车ID不能为空");
        }

        Cart exist = cartService.getById(cart.getId());
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }

        if (cart.getNumber() != null && cart.getNumber() <= 0) {
            throw new BusinessException("数量必须大于0");
        }

        Cart update = new Cart();
        update.setId(cart.getId());
        update.setNumber(cart.getNumber());
        cartService.updateById(update);
        return Result.success("修改成功", null);
    }

    /**
     * 删除购物车项
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Cart exist = cartService.getById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        cartService.removeById(id);
        return Result.success("删除成功", null);
    }
}

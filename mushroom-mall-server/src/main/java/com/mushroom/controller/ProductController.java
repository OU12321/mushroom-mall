package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushroom.annotation.RequireRole;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.Category;
import com.mushroom.entity.Product;
import com.mushroom.entity.User;
import com.mushroom.service.CategoryService;
import com.mushroom.service.ProductService;
import com.mushroom.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * 商品控制器
 * 商家管理自家商品，管理员审核/全局管理，用户浏览
 */
@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    /**
     * 填充商品的分类名和商家名 (非数据库字段)
     */
    private void fillExtraFields(List<Product> products) {
        if (products == null || products.isEmpty()) return;
        for (Product p : products) {
            if (p.getCategoryId() != null) {
                Category cat = categoryService.getById(p.getCategoryId());
                if (cat != null) p.setCategoryName(cat.getName());
            }
            if (p.getMerchantId() != null) {
                User merchant = userService.getById(p.getMerchantId());
                if (merchant != null) p.setMerchantName(merchant.getName());
            }
        }
    }

    /**
     * 分页查询商品 (管理端)
     * 商家只查自家，管理员可查全部，可按审核状态筛选
     */
    @RequireRole({0, 1})
    @GetMapping("/page")
    public Result<Page<Product>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Long merchantId,
            HttpServletRequest request) {

        Integer role = (Integer) request.getAttribute("role");
        Long userId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!StringUtils.isEmpty(name), Product::getName, name);
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId);
        wrapper.eq(auditStatus != null, Product::getAuditStatus, auditStatus);

        // 商家只能看自家商品
        if (role == 1) {
            wrapper.eq(Product::getMerchantId, userId);
        } else if (merchantId != null) {
            wrapper.eq(Product::getMerchantId, merchantId);
        }

        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> pageInfo = productService.page(new Page<>(page, pageSize), wrapper);
        fillExtraFields(pageInfo.getRecords());
        return Result.success(pageInfo);
    }

    /**
     * 用户端浏览商品列表 (只展示审核通过且上架的商品)
     *
     * 除审核与上架状态外，还要求所属商家账号有效：商家被删除后其商品不应继续对外展示，
     * 这里用子查询兜底过滤，避免历史遗留的孤儿商品出现在商城中。
     */
    @GetMapping("/list")
    public Result<Page<Product>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name) {

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getAuditStatus, 1);
        wrapper.eq(Product::getStatus, 1);
        wrapper.inSql(Product::getMerchantId,
                "SELECT id FROM user WHERE role = 1 AND is_deleted = 0");
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId);
        wrapper.like(!StringUtils.isEmpty(name), Product::getName, name);
        wrapper.orderByDesc(Product::getSaleCount, Product::getCreateTime);

        Page<Product> pageInfo = productService.page(new Page<>(page, pageSize), wrapper);
        fillExtraFields(pageInfo.getRecords());
        return Result.success(pageInfo);
    }

    /**
     * 商品详情（管理端/商家端，需要登录）
     */
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return Result.success(product);
    }

    /**
     * 商品详情（用户端公开浏览，无需登录）
     *
     * 所属商家账号已被删除的商品不再对外开放，避免用户点进详情后无法下单。
     */
    @GetMapping("/detail/{id}")
    public Result<Product> getByIdForUser(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        // 未审核 / 已下架的商品不能通过 id 直接访问详情。
        // 原实现只校验商品与商家是否存在，商家下架后知道 id 仍能看到详情页。
        if (product.getAuditStatus() == null || product.getAuditStatus() != 1
                || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("该商品已下架");
        }
        if (product.getMerchantId() == null || userService.getById(product.getMerchantId()) == null) {
            throw new BusinessException("该商品已下架");
        }
        fillExtraFields(Collections.singletonList(product));
        return Result.success(product);
    }

    /**
     * 商家新增商品
     */
    @RequireRole(1)
    @PostMapping
    public Result<Void> save(@RequestBody Product product, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        product.setMerchantId(userId);
        product.setAuditStatus(0);
        product.setStatus(0);
        if (product.getSaleCount() == null) {
            product.setSaleCount(0);
        }
        productService.save(product);
        return Result.success("新增成功，请等待审核", null);
    }
    /**
     * 商家修改自家商品
     */
    @RequireRole(1)
    @PutMapping
    public Result<Void> update(@RequestBody Product product, HttpServletRequest request) {
        if (product.getId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        Long userId = (Long) request.getAttribute("userId");
        Product exist = productService.getById(product.getId());
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!exist.getMerchantId().equals(userId)) {
            throw new BusinessException("只能修改自家商品");
        }
        // 修改后需重新审核
        product.setAuditStatus(0);
        productService.updateById(product);
        return Result.success("修改成功，请等待审核", null);
    }
    /**
     * 管理员审核商品
     * auditStatus: 1=通过 2=驳回
     */
    @RequireRole(0)
    @PutMapping("/audit/{id}")
    public Result<Void> audit(@PathVariable Long id, @RequestParam Integer auditStatus) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        Product update = new Product();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        // 审核通过自动上架
        if (auditStatus == 1) {
            update.setStatus(1);
        }
        productService.updateById(update);
        return Result.success(auditStatus == 1 ? "审核通过" : "已驳回", null);
    }

    /**
     * 商家上下架
     */
    @RequireRole(1)
    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Product exist = productService.getById(id);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!exist.getMerchantId().equals(userId)) {
            throw new BusinessException("只能操作自家商品");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(status);
        productService.updateById(update);
        return Result.success(status == 1 ? "已上架" : "已下架", null);
    }

    /**
     * 商家删除自家商品
     */
    @RequireRole(1)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Product exist = productService.getById(id);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!exist.getMerchantId().equals(userId)) {
            throw new BusinessException("只能删除自家商品");
        }
        productService.removeById(id);
        return Result.success("删除成功", null);
    }
}

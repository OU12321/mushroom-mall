package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushroom.annotation.RequireRole;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.Category;
import com.mushroom.entity.Product;
import com.mushroom.mapper.ProductMapper;
import com.mushroom.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 商品分类控制器
 * 管理员可增删改查，删除时检查是否有关联商品 (分类关联保护)
 * 权限：本控制器整体限管理员访问；/category/list 已加入拦截白名单，供用户端/商家端公开读取
 */
@Slf4j
@RestController
@RequestMapping("/category")
@RequireRole(0)
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 分页查询分类 (管理端)
     */
    @GetMapping("/page")
    public Result<Page<Category>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!StringUtils.isEmpty(name), Category::getName, name);
        wrapper.orderByAsc(Category::getSort);

        Page<Category> pageInfo = categoryService.page(new Page<>(page, pageSize), wrapper);
        return Result.success(pageInfo);
    }

    /**
     * 查询所有分类 (下拉选择/前端展示用，无需分页)
     */
    @GetMapping("/list")
    public Result list() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSort);
        return Result.success(categoryService.list(wrapper));
    }

    /**
     * 新增分类
     */
    @PostMapping
    public Result<Void> save(@RequestBody Category category) {
        if (StringUtils.isEmpty(category.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        // 检查分类名是否重复
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, category.getName());
        if (categoryService.count(wrapper) > 0) {
            throw new BusinessException("该分类名称已存在");
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        categoryService.save(category);
        return Result.success("新增成功", null);
    }

    /**v
     * 修改分类
     */
    @PutMapping
    public Result<Void> update(@RequestBody Category category) {
        if (category.getId() == null) {
            throw new BusinessException("分类ID不能为空");
        }
        // 检查新名称是否与其他分类重名
        if (!StringUtils.isEmpty(category.getName())) {
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Category::getName, category.getName());
            wrapper.ne(Category::getId, category.getId());
            if (categoryService.count(wrapper) > 0) {
                throw new BusinessException("该分类名称已存在");
            }
        }
        categoryService.updateById(category);
        return Result.success("修改成功", null);
    }

    /**
     * 删除分类 (分类关联保护: 分类下有商品则不允许删除)
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否有关联商品
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getCategoryId, id);
        long count = productMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("该分类下有 " + count + " 个商品，无法删除");
        }
        categoryService.removeById(id);
        return Result.success("删除成功", null);
    }
}

package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushroom.annotation.RequireRole;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.Banner;
import com.mushroom.service.BannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 轮播图控制器
 * 管理员增删改查，前端获取启用列表
 * 权限：本控制器整体限管理员访问；/banner/list 已加入拦截白名单，供用户端首页公开读取
 */
@Slf4j
@RestController
@RequestMapping("/banner")
@RequireRole(0)
public class BannerController {

    @Autowired
    private BannerService bannerService;

    /**
     * 管理员分页查询
     */
    @GetMapping("/page")
    public Result<Page<Banner>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSort);
        Page<Banner> pageInfo = bannerService.page(new Page<>(page, pageSize), wrapper);
        return Result.success(pageInfo);
    }

    /**
     * 启用轮播图列表 (前端使用，status=1，按sort排序)
     */
    @GetMapping("/list")
    public Result<List<Banner>> list() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1);
        wrapper.orderByAsc(Banner::getSort);
        List<Banner> list = bannerService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 管理员新增轮播图
     */
    @PostMapping
    public Result<Void> save(@RequestBody Banner banner) {
        if (banner.getTitle() == null || banner.getTitle().trim().isEmpty()) {
            throw new BusinessException("标题不能为空");
        }
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        bannerService.save(banner);
        return Result.success("新增成功", null);
    }

    /**
     * 管理员修改轮播图
     */
    @PutMapping
    public Result<Void> update(@RequestBody Banner banner) {
        if (banner.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        bannerService.updateById(banner);
        return Result.success("修改成功", null);
    }

    /**
     * 切换轮播图启用状态
     */
    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Banner update = new Banner();
        update.setId(id);
        update.setStatus(status);
        bannerService.updateById(update);
        return Result.success(status == 1 ? "已启用" : "已禁用", null);
    }

    /**
     * 管理员删除轮播图
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.removeById(id);
        return Result.success("删除成功", null);
    }
}

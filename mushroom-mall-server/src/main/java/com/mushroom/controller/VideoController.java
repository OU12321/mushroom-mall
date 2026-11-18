package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushroom.annotation.RequireRole;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.Video;
import com.mushroom.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 宣传视频控制器
 * 管理员增删改查，用户端获取启用列表
 * 权限：本控制器整体限管理员访问；/video/list 已加入拦截白名单，供用户端首页公开读取
 */
@Slf4j
@RestController
@RequestMapping("/video")
@RequireRole(0)
public class VideoController {

    @Autowired
    private VideoService videoService;

    /**
     * 管理员分页查询
     */
    @GetMapping("/page")
    public Result<Page<Video>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Video::getSort);
        Page<Video> pageInfo = videoService.page(new Page<>(page, pageSize), wrapper);
        return Result.success(pageInfo);
    }

    /**
     * 启用视频列表 (用户端使用，status=1，按sort排序)
     */
    @GetMapping("/list")
    public Result<List<Video>> list() {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getStatus, 1);
        wrapper.orderByAsc(Video::getSort);
        List<Video> list = videoService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 管理员新增视频
     */
    @PostMapping
    public Result<Void> save(@RequestBody Video video) {
        if (video.getTitle() == null || video.getTitle().trim().isEmpty()) {
            throw new BusinessException("标题不能为空");
        }
        if (video.getVideo() == null || video.getVideo().trim().isEmpty()) {
            throw new BusinessException("视频不能为空");
        }
        if (video.getSort() == null) {
            video.setSort(0);
        }
        if (video.getStatus() == null) {
            video.setStatus(1);
        }
        videoService.save(video);
        return Result.success("新增成功", null);
    }

    /**
     * 管理员修改视频
     */
    @PutMapping
    public Result<Void> update(@RequestBody Video video) {
        if (video.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        videoService.updateById(video);
        return Result.success("修改成功", null);
    }

    /**
     * 切换视频启用状态
     */
    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Video update = new Video();
        update.setId(id);
        update.setStatus(status);
        videoService.updateById(update);
        return Result.success(status == 1 ? "已启用" : "已禁用", null);
    }

    /**
     * 管理员删除视频
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        videoService.removeById(id);
        return Result.success("删除成功", null);
    }
}

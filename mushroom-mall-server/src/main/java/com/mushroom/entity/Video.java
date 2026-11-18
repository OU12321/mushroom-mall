package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宣传视频表
 * 管理员上传管理宣传视频，用户端首页展示
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video")
public class Video extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 视频标题 */
    private String title;

    /** 视频URL */
    private String video;

    /** 封面图URL */
    private String cover;

    /** 视频描述 */
    private String description;

    /** 排序 */
    private Integer sort;

    /** 状态: 0=禁用 1=启用 */
    private Integer status;
}

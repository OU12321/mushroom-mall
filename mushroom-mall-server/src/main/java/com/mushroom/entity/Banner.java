package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 轮播图表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("banner")
public class Banner extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 轮播图标题 */
    private String title;

    /** 图片URL */
    private String image;

    /** 排序 */
    private Integer sort;

    /** 状态: 0=禁用 1=启用 */
    private Integer status;
}

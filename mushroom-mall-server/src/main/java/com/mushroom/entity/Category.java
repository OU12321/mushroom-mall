package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 (牛肝菌/松茸/鸡枞/羊肚菌...) */
    private String name;

    /** 分类排序 */
    private Integer sort;

    /** 分类图标 */
    private String icon;
}

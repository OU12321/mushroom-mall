package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * 商品表
 * 商家上架，需管理员审核通过才展示 (野生菌食用安全把关)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 分类ID */
    private Long categoryId;

    /** 所属商家ID (商家只能管理自家商品) */
    private Long merchantId;

    /** 价格 (元) */
    private BigDecimal price;

    /** 商品图片URL */
    private String image;

    /** 商品描述 (富文本) */
    private String description;

    /** 产地 (高山采摘地) */
    private String origin;

    /** 采摘时间 */
    private String harvestTime;

    /** 食用方法 */
    private String cookingMethod;

    /** 审核状态: 0=待审核 1=已通过 2=已驳回 (管理员审核) */
    private Integer auditStatus;

    /** 上架状态: 0=下架 1=上架 */
    private Integer status;

    /** 销量 */
    private Integer saleCount;

    /** 库存 (0=售罄, >0=有库存) */
    private Integer stock;

    /** 分类名称 (非数据库字段，用于前端展示) */
    @TableField(exist = false)
    private String categoryName;

    /** 商家名称 (非数据库字段，用于前端展示) */
    @TableField(exist = false)
    private String merchantName;
}

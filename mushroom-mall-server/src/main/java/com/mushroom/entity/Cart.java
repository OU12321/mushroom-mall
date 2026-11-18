package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * 购物车表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart")
public class Cart extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 商家ID */
    private Long merchantId;

    /** 商品名称 (冗余) */
    private String productName;

    /** 商品图片 (冗余) */
    private String productImage;

    /** 单价 (冗余) */
    private BigDecimal price;

    /** 数量 */
    private Integer number;
}

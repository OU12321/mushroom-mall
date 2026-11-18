package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * 订单明细表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_detail")
public class OrderDetail extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 (冗余, 防止商品改名) */
    private String productName;

    /** 商品图片 (冗余) */
    private String productImage;

    /** 商品单价 (冗余) */
    private BigDecimal price;

    /** 购买数量 */
    private Integer number;

    /** 金额小计 */
    private BigDecimal amount;
}

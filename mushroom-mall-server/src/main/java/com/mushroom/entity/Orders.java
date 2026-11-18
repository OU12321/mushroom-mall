package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Orders extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 下单用户ID */
    private Long userId;

    /** 商家ID (订单归属商家) */
    private Long merchantId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 收货人姓名 */
    private String consignee;

    /** 收货电话 */
    private String phone;

    /** 收货地址 */
    private String address;

    /** 订单状态: 1=待付款 2=待发货 3=待收货 4=已完成 5=已取消 6=退款中 7=已退款 */
    private Integer status;

    /** 支付方式: 1=微信 2=支付宝 */
    private Integer payMethod;

    /** 支付时间 */
    private String payTime;

    /** 发货时间 */
    private String shipTime;

    /** 收货时间 */
    private String receiveTime;

    /** 订单备注 */
    private String remark;

    /** 订单明细列表 (非数据库字段，用于详情返回) */
    @TableField(exist = false)
    private List<OrderDetail> orderDetails;

    /** 是否已评价 (非数据库字段，前端用) */
    @TableField(exist = false)
    private Boolean reviewed;
}

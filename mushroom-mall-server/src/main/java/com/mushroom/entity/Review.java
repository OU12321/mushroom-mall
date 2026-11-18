package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品评价表
 * 用户评价, 商家可回复但不能删除, 管理员可删违规评价
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review")
public class Review extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 订单ID */
    private Long orderId;

    /** 评价用户ID */
    private Long userId;

    /** 评价用户昵称 (冗余) */
    private String userName;

    /** 评价用户头像 (冗余) */
    private String userAvatar;

    /** 评分 1-5星 */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 评价图片 (多图逗号分隔) */
    private String images;

    /** 商家回复 */
    private String reply;
}

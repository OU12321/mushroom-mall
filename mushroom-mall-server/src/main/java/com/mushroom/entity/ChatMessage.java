package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天消息表
 * 用户(role=2) ↔ 商家(role=1) 在线客服聊天
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送者ID */
    private Long senderId;

    /** 发送者角色: 1=商家 2=用户 */
    private Integer senderRole;

    /** 接收者ID */
    private Long receiverId;

    /** 接收者角色: 1=商家 2=用户 */
    private Integer receiverRole;

    /** 消息内容 */
    private String content;

    /** 是否已读: 0=未读 1=已读 */

    @com.baomidou.mybatisplus.annotation.TableField("is_read")
    private Integer isRead;
}

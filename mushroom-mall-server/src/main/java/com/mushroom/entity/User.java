package com.mushroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表 (统一存储三角色)
 *
 * role 字段区分角色:
 *   0 = 管理员  全局管控
 *   1 = 商家    店铺经营 (需审核)
 *   2 = 普通用户 购物互动
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名/登录账号 */
    private String username;

    /** 密码 (MD5加密存储) */
    private String password;

    /** 真实姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 性别: 0=男 1=女 */
    private Integer sex;

    /** 头像URL */
    private String avatar;

    /** 角色: 0=管理员 1=商家 2=普通用户 */
    private Integer role;

    /** 账号状态: 0=禁用 1=正常 */
    private Integer status;

    /** 商家专属 - 审核状态: 0=待审核 1=已通过 2=已驳回 (role=1时有值) */
    private Integer auditStatus;
}

package com.mushroom.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解
 *
 * 标注在 Controller 类或方法上，用于限制可访问的角色。
 * 方法上的注解优先于类上的注解；未标注则不做角色限制（仍需登录，取决于拦截白名单）。
 *
 * 角色取值：0=管理员 1=商家 2=普通用户
 *
 * 用法示例：
 *   // 整个 Controller 仅管理员可访问
 *   @RequireRole(0)
 *   public class UserController { ... }
 *
 *   // 单个方法允许商家或管理员访问
 *   @RequireRole({0, 1})
 *   public Result page(...) { ... }
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 允许访问的角色，满足其中任意一个即可 */
    int[] value();
}

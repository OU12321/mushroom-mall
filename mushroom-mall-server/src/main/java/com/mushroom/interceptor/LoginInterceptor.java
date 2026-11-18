package com.mushroom.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mushroom.annotation.RequireRole;
import com.mushroom.common.Result;
import com.mushroom.entity.User;
import com.mushroom.service.UserService;
import com.mushroom.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 *
 * 1. 校验 JWT token，将 userId / role 存入 request attribute 供 Controller 使用
 * 2. 读取 {@link RequireRole} 注解做角色校验，防止普通用户调用管理员/商家接口
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 静态资源（前端页面、js/css、上传的商品图片）没有 token 概念，直接放行。
        // 这类请求由 ResourceHttpRequestHandler 处理，不是 HandlerMethod。
        // 用类型判断代替逐个罗列路径：以后前端 dist 目录结构变了也不会漏放行。
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 从请求头获取 token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !jwtUtils.isValid(token)) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }

        // 将用户信息存入 request，Controller 可通过 request.getAttribute 获取
        Long userId = jwtUtils.getUserId(token);
        Integer role = jwtUtils.getRole(token);
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);

        // token 有效不等于账号还能用：账号可能已被管理员禁用或删除，
        // 而 JWT 默认 7 天内无法吊销 —— 原实现只在登录时校验一次 status，
        // 导致「禁用账号」后对方凭旧 token 还能继续下单 7 天。
        // 这里每次请求校验一次账号状态（主键查询，开销很小；如需进一步优化可加本地缓存）。
        User current = userService.getById(userId);
        if (current == null || current.getStatus() == null || current.getStatus() != 1) {
            writeError(response, 401, "账号已被禁用或不存在，请重新登录");
            return false;
        }

        // 角色权限校验：仅当接口标注了 @RequireRole 时才生效
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            if (requireRole == null) {
                // 方法上没标注则回退到类上的标注
                requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
            }
            if (requireRole != null && !hasPermission(role, requireRole.value())) {
                log.warn("越权访问被拒绝: userId={}, role={}, uri={}", userId, role, request.getRequestURI());
                writeError(response, 403, "无权访问该功能");
                return false;
            }
        }
        return true;
    }

    /** 判断当前角色是否在允许列表内 */
    private boolean hasPermission(Integer role, int[] allowedRoles) {
        if (role == null || allowedRoles == null) {
            return false;
        }
        for (int allowed : allowedRoles) {
            if (role == allowed) {
                return true;
            }
        }
        return false;
    }

    /** 统一输出 JSON 错误响应 */
    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}

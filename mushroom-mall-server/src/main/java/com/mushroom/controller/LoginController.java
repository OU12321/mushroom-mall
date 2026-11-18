package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.User;
import com.mushroom.mapper.UserMapper;
import com.mushroom.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录/注册控制器
 */
@Slf4j
@RestController
public class LoginController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            throw new BusinessException("用户名或密码不能为空");
        }
        // 统一小写查询（与注册时存小写对齐）
        username = username.toLowerCase();

        // MD5 加密密码
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null || !user.getPassword().equals(md5Password)) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        // 商家入驻审核：待审核(0) 与 已驳回(2) 不允许登录。
        // 原实现完全不看 audit_status，管理员点「驳回」只把 status 置 0，
        // 而 status 又能在后台随时改回 1，等于审核没有实际约束力。
        // audit_status 为空的历史商家账号按已通过处理，避免老数据被误锁在门外。
        if (user.getRole() != null && user.getRole() == 1
                && user.getAuditStatus() != null && user.getAuditStatus() != 1) {
            throw new BusinessException(user.getAuditStatus() == 0
                    ? "商家账号待管理员审核，暂时无法登录"
                    : "商家入驻申请已被驳回，请联系管理员");
        }

        // 生成 token
        String token = jwtUtils.generateToken(user.getId(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("name", user.getName());
        result.put("role", user.getRole());
        result.put("avatar", user.getAvatar());
        return Result.success("登录成功", result);
    }

    /**
     * 注册 (固定注册为普通用户 role=2，不接受客户端指定的角色)
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody User user) {
        user.setUsername(user.getUsername().toLowerCase());
        Long existAll = userMapper.countAllByUsername(user.getUsername());
        if (existAll > 0) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, user.getUsername());
            Long activeCount = userMapper.selectCount(wrapper);
            if (activeCount > 0) {
                throw new BusinessException("用户名已存在");
            }
            userMapper.physicalDeleteByUsername(user.getUsername());
        }
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        // 公开注册一律为普通用户。
        // 原实现是「角色为空时兜底为 2」，即完全信任请求体里的 role：
        // 构造 {"username":"x","password":"x","role":0} 就能直接注册出管理员账号。
        // 商家账号只能由管理员在后台创建（见 UserController.save）。
        user.setRole(2);
        user.setAuditStatus(null);
        user.setStatus(1);
        userMapper.insert(user);
        return Result.success("注册成功", null);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/user/info")
    public Result<User> userInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return Result.success(user);
    }

    /**
     * 退出登录 (前端清除 token 即可)
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功", null);
    }
}

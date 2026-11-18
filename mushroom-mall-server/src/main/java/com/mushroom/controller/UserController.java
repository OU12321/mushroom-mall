package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushroom.annotation.RequireRole;
import com.mushroom.common.Result;
import com.mushroom.common.exception.BusinessException;
import com.mushroom.entity.Cart;
import com.mushroom.entity.Orders;
import com.mushroom.entity.Product;
import com.mushroom.entity.User;
import com.mushroom.mapper.UserMapper;
import com.mushroom.service.CartService;
import com.mushroom.service.OrdersService;
import com.mushroom.service.ProductService;
import com.mushroom.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户管理控制器
 * 默认仅管理员可访问（类上 @RequireRole(0)）；
 * 例外：个人资料的修改（PUT /user）同时对本人在线开放，见 update() 的方法级注解。
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequireRole(0)
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrdersService ordersService;

    /**
     * 分页查询用户
     * 支持按用户名模糊搜索、角色筛选
     */
    @GetMapping("/page")
    public Result<Page<User>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer role) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!StringUtils.isEmpty(name), User::getName, name);
        wrapper.eq(role != null, User::getRole, role);
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> pageInfo = userService.page(new Page<>(page, pageSize), wrapper);
        // 清除密码再返回
        pageInfo.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(pageInfo);
    }

    /**
     * 新增用户 (管理员后台创建账号)
     *
     * 用户名唯一性需要连带检查"已逻辑删除"的历史行：
     * user 表的 uk_username 是唯一索引，而索引本身不区分 is_deleted，
     * 若只按未删除行判断，插入时会撞唯一索引抛 DuplicateKeyException，
     * 被全局异常处理器兜底成"系统繁忙"，用户看不出真实原因。
     * 这里参照注册接口的处理方式：仅剩历史删除行时先物理清除再插入。
     */
    @PostMapping
    public Result<Void> save(@RequestBody User user) {
        if (StringUtils.isEmpty(user.getUsername())) {
            throw new BusinessException("账号不能为空");
        }
        String username = user.getUsername().trim().toLowerCase();
        user.setUsername(username);

        // 查全量（含已逻辑删除行，绕过 @TableLogic 过滤）
        Long existAll = userMapper.countAllByUsername(username);
        if (existAll != null && existAll > 0) {
            // 存在仍然有效的同名账号 → 直接拒绝
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            long activeCount = userService.count(wrapper);
            if (activeCount > 0) {
                throw new BusinessException("用户名已存在");
            }
            // 只剩历史删除行 → 物理清除，腾出唯一索引
            userMapper.physicalDeleteByUsername(username);
        }

        // 默认密码 123456 (如果未填)
        String pwd = StringUtils.isEmpty(user.getPassword()) ? "123456" : user.getPassword();
        user.setPassword(DigestUtils.md5DigestAsHex(pwd.getBytes()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getRole() == null) {
            user.setRole(2); // 默认普通用户
        }
        // 商家需审核
        if (user.getRole() == 1 && user.getAuditStatus() == null) {
            user.setAuditStatus(1); // 管理员直接创建默认通过
        }
        userService.save(user);
        log.info("管理员创建账号 {} (角色 {})", username, user.getRole());
        return Result.success("新增成功", null);
    }

    /**
     * 修改用户信息
     *
     * 方法级 @RequireRole 覆盖类上的管理员限制，因为个人中心「保存修改」
     * 三类角色都要用。放开权限的同时必须做两件事，否则等于重新打开提权漏洞：
     *   1. 归属锁定：非管理员只能改自己（忽略请求里的 id，强制用 token 里的 userId）
     *   2. 字段白名单：非管理员提交的 username / role / status / auditStatus 一律置空；
     *      MyBatis-Plus 的 updateById 会忽略 null 字段，所以这些字段不会被覆盖，
     *      即可改的只剩 name / phone / sex / avatar / password
     */
    @PutMapping
    @RequireRole({0, 1, 2})
    public Result<Void> update(@RequestBody User user, HttpServletRequest request) {
        if (user.getId() == null) {
            throw new BusinessException("用户ID不能为空");
        }

        Long currentUserId = (Long) request.getAttribute("userId");
        Integer currentRole = (Integer) request.getAttribute("role");

        if (currentRole == null || currentRole != 0) {
            // 非管理员：强制只能操作本人，并清掉越权字段
            if (currentUserId == null) {
                throw new BusinessException("未登录或登录已过期");
            }
            user.setId(currentUserId);
            user.setUsername(null);     // 不许改登录名
            user.setRole(null);         // 不许提权
            user.setStatus(null);       // 不许自行解禁
            user.setAuditStatus(null);  // 不许自审
        }

        // 如果修改了密码，重新 MD5 加密
        if (!StringUtils.isEmpty(user.getPassword())) {
            user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        } else {
            // 不改密码则置空，避免覆盖
            user.setPassword(null);
        }
        userService.updateById(user);
        return Result.success("修改成功", null);
    }

    /**
     * 修改账号状态 (启用/禁用)
     */
    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userService.updateById(user);
        return Result.success(status == 1 ? "已启用" : "已禁用", null);
    }

    /**
     * 商家入驻审核 (通过/驳回)
     * @param auditStatus 1=通过 2=驳回
     */
    @PutMapping("/audit/{id}")
    public Result<Void> audit(@PathVariable Long id, @RequestParam Integer auditStatus) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getRole() != 1) {
            throw new BusinessException("仅商家账号需要审核");
        }
        User update = new User();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        // 审核通过则启用账号，驳回则禁用
        update.setStatus(auditStatus == 1 ? 1 : 0);
        userService.updateById(update);
        return Result.success(auditStatus == 1 ? "审核通过" : "已驳回", null);
    }

    /**
     * 删除用户 (逻辑删除)
     *
     * 商家账号被删除后，其名下商品若继续留在商品表里，用户端仍能浏览和下单，
     * 会形成"商家已注销、商品还在卖"的孤儿数据。因此这里按角色做级联处理：
     * 商家账号连带删除名下商品并清理购物车中对应条目，普通账号清理其购物车。
     *
     * 该方法跨 product / cart / user 三张表写入，必须加事务：
     * 中途任何一步失败都会整体回滚，避免出现"商家已删、商品仍在售"这类半成品数据。
     */
    @Transactional
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Integer role = user.getRole();
        if (role != null && role == 1) {
            // 还有未完成订单时不允许删除，否则订单将无法继续履约
            long unfinished = ordersService.count(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getMerchantId, id)
                    .in(Orders::getStatus, 1, 2, 3, 6));
            if (unfinished > 0) {
                throw new BusinessException("该商家还有 " + unfinished + " 笔未完成订单，请先处理后再删除");
            }
            // 级联删除名下全部商品 (走 MyBatis-Plus 逻辑删除)
            productService.remove(new LambdaQueryWrapper<Product>()
                    .eq(Product::getMerchantId, id));
            // 清理所有用户购物车中属于该商家的商品
            cartService.remove(new LambdaQueryWrapper<Cart>()
                    .eq(Cart::getMerchantId, id));
            log.info("删除商家账号 {}，名下商品与购物车条目已级联清理", user.getUsername());
        } else if (role != null && role == 2) {
            // 普通用户被删除时同样要检查未完成订单：
            // 原实现只清购物车，用户删掉后待发货/待收货的订单仍在，商家发货无人签收
            long unfinished = ordersService.count(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getUserId, id)
                    .in(Orders::getStatus, 1, 2, 3, 6));
            if (unfinished > 0) {
                throw new BusinessException("该用户还有 " + unfinished + " 笔未完成订单，请先处理后再删除");
            }
            cartService.remove(new LambdaQueryWrapper<Cart>()
                    .eq(Cart::getUserId, id));
        }

        userService.removeById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 根据 ID 查询用户详情
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
}

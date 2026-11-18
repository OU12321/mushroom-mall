package com.mushroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mushroom.entity.User;

/**
 * 用户服务接口
 * 涵盖管理员、商家、普通用户三类账号
 */
public interface UserService extends IService<User> {
}

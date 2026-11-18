package com.mushroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushroom.entity.Cart;
import com.mushroom.mapper.CartMapper;
import com.mushroom.service.CartService;
import org.springframework.stereotype.Service;

/**
 * 购物车服务实现
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {
}

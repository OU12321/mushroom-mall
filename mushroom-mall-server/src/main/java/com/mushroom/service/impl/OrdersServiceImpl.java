package com.mushroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushroom.entity.Orders;
import com.mushroom.mapper.OrdersMapper;
import com.mushroom.service.OrdersService;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现
 */
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
}

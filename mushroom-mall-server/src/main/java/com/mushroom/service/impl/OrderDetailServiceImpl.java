package com.mushroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushroom.entity.OrderDetail;
import com.mushroom.mapper.OrderDetailMapper;
import com.mushroom.service.OrderDetailService;
import org.springframework.stereotype.Service;

/**
 * 订单明细服务实现
 */
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
}

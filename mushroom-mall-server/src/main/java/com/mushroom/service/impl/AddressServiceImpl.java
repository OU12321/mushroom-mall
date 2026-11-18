package com.mushroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushroom.entity.Address;
import com.mushroom.mapper.AddressMapper;
import com.mushroom.service.AddressService;
import org.springframework.stereotype.Service;

/**
 * 收货地址服务实现
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {
}

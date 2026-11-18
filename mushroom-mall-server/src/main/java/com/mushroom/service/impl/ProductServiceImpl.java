package com.mushroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushroom.entity.Product;
import com.mushroom.mapper.ProductMapper;
import com.mushroom.service.ProductService;
import org.springframework.stereotype.Service;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}

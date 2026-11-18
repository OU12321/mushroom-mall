package com.mushroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushroom.entity.Category;
import com.mushroom.mapper.CategoryMapper;
import com.mushroom.service.CategoryService;
import org.springframework.stereotype.Service;

/**
 * 分类服务实现
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
}

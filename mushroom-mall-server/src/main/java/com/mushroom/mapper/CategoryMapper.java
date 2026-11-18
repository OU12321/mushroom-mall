package com.mushroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mushroom.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}

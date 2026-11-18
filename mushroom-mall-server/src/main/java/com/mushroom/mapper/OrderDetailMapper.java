package com.mushroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mushroom.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {
}

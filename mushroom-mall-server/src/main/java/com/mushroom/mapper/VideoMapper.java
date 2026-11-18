package com.mushroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mushroom.entity.Video;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宣传视频 Mapper
 */
@Mapper
public interface VideoMapper extends BaseMapper<Video> {
}

package com.mushroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushroom.entity.Video;
import com.mushroom.mapper.VideoMapper;
import com.mushroom.service.VideoService;
import org.springframework.stereotype.Service;

/**
 * 宣传视频服务实现
 */
@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {
}

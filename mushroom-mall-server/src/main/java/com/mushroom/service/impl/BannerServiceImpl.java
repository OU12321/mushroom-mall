package com.mushroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushroom.entity.Banner;
import com.mushroom.mapper.BannerMapper;
import com.mushroom.service.BannerService;
import org.springframework.stereotype.Service;

/**
 * 轮播图服务实现
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {
}

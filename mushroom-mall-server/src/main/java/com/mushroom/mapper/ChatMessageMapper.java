package com.mushroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mushroom.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}

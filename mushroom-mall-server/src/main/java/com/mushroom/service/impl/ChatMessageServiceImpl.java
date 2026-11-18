package com.mushroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushroom.entity.ChatMessage;
import com.mushroom.mapper.ChatMessageMapper;
import com.mushroom.service.ChatMessageService;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {
}

package com.mushroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mushroom.common.Result;
import com.mushroom.entity.ChatMessage;
import com.mushroom.entity.User;
import com.mushroom.service.ChatMessageService;
import com.mushroom.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天 REST 接口
 * 获取聊天历史 / 未读数 / 联系人列表 / 标记已读
 */
@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private UserService userService;

    /**
     * 获取与某人的聊天历史
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(
            @RequestParam Long otherId,
            @RequestParam Integer otherRole,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");

        // 查询双向消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(ChatMessage::getSenderId, userId)
                        .eq(ChatMessage::getSenderRole, role)
                        .eq(ChatMessage::getReceiverId, otherId)
                        .eq(ChatMessage::getReceiverRole, otherRole))
                .or(w2 -> w2.eq(ChatMessage::getSenderId, otherId)
                        .eq(ChatMessage::getSenderRole, otherRole)
                        .eq(ChatMessage::getReceiverId, userId)
                        .eq(ChatMessage::getReceiverRole, role))
        );
        wrapper.orderByAsc(ChatMessage::getCreateTime);
        wrapper.last("LIMIT 200");

        List<ChatMessage> messages = chatMessageService.list(wrapper);

        // 自动标记已读（对方发给我的消息）
        LambdaUpdateWrapper<ChatMessage> readWrapper = new LambdaUpdateWrapper<>();
        readWrapper.eq(ChatMessage::getSenderId, otherId)
                .eq(ChatMessage::getSenderRole, otherRole)
                .eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getReceiverRole, role)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1);
        chatMessageService.update(readWrapper);

        List<Map<String, Object>> result = messages.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("senderId", m.getSenderId());
            map.put("senderRole", m.getSenderRole());
            map.put("receiverId", m.getReceiverId());
            map.put("receiverRole", m.getReceiverRole());
            map.put("content", m.getContent());
            map.put("isRead", m.getIsRead());
            map.put("createTime", m.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 获取未读消息总数
     */
    @GetMapping("/unread")
    public Result<Map<String, Object>> unread(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getReceiverRole, role)
                .eq(ChatMessage::getIsRead, 0);
        long count = chatMessageService.count(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        return Result.success(result);
    }

    /**
     * 获取联系人列表
     * - 普通用户(role=2): 显示所有商家(role=1),即使暂无聊天记录也可发起对话
     * - 商家(role=1): 显示所有聊过天的用户
     */
    @GetMapping("/contacts")
    public Result<List<Map<String, Object>>> contacts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");

        // 1. 查所有我发出/收到的消息,提取已有联系人
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getSenderRole, role)
                .or()
                .eq(ChatMessage::getReceiverId, userId).eq(ChatMessage::getReceiverRole, role)
        );
        wrapper.orderByDesc(ChatMessage::getCreateTime);
        List<ChatMessage> allMessages = chatMessageService.list(wrapper);

        // 2. 把已有联系人放进 Map (key = otherId_otherRole)
        Map<String, Map<String, Object>> contactMap = new LinkedHashMap<>();
        for (ChatMessage msg : allMessages) {
            Long otherId;
            Integer otherRole;
            if (msg.getSenderId().equals(userId) && msg.getSenderRole().equals(role)) {
                otherId = msg.getReceiverId();
                otherRole = msg.getReceiverRole();
            } else {
                otherId = msg.getSenderId();
                otherRole = msg.getSenderRole();
            }
            String key = otherId + "_" + otherRole;
            if (!contactMap.containsKey(key)) {
                Map<String, Object> contact = new HashMap<>();
                contact.put("otherId", otherId);
                contact.put("otherRole", otherRole);
                contact.put("lastMessage", msg.getContent());
                contact.put("lastTime", msg.getCreateTime());
                // 查用户名
                User otherUser = userService.getById(otherId);
                contact.put("name", otherUser != null ? otherUser.getName() : "未知用户");
                contact.put("avatar", otherUser != null ? otherUser.getAvatar() : null);
                // 未读数
                LambdaQueryWrapper<ChatMessage> unreadWrapper = new LambdaQueryWrapper<>();
                unreadWrapper.eq(ChatMessage::getSenderId, otherId)
                        .eq(ChatMessage::getSenderRole, otherRole)
                        .eq(ChatMessage::getReceiverId, userId)
                        .eq(ChatMessage::getReceiverRole, role)
                        .eq(ChatMessage::getIsRead, 0);
                contact.put("unreadCount", chatMessageService.count(unreadWrapper));
                contactMap.put(key, contact);
            }
        }

        // 3. 普通用户:如果联系人列表为空,展示所有商家作为可联系对象
        if (role != null && role == 2 && contactMap.isEmpty()) {
            LambdaQueryWrapper<User> merchantWrapper = new LambdaQueryWrapper<>();
            merchantWrapper.eq(User::getRole, 1).eq(User::getIsDeleted, 0);
            List<User> merchants = userService.list(merchantWrapper);
            for (User merchant : merchants) {
                Map<String, Object> contact = new HashMap<>();
                contact.put("otherId", merchant.getId());
                contact.put("otherRole", 1);
                contact.put("name", merchant.getName());
                contact.put("avatar", merchant.getAvatar());
                contact.put("lastMessage", "暂无消息，点击开始对话");
                contact.put("lastTime", null);
                contact.put("unreadCount", 0);
                contactMap.put(merchant.getId() + "_1", contact);
            }
        }

        return Result.success(new ArrayList<>(contactMap.values()));
    }
}

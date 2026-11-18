package com.mushroom.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mushroom.entity.ChatMessage;
import com.mushroom.service.ChatMessageService;
import com.mushroom.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 在线客服聊天
 * 用户(role=2) ↔ 商家(role=1) 实时消息推送
 *
 * 连接方式：ws://host/api/ws/chat?token=<JWT>
 *
 * 安全说明：
 * 原实现从 URL 参数取 userId/role、从消息体取 senderId/senderRole，均来自客户端，
 * 任何人都能声称自己是任意用户或商家并往聊天记录里写伪造消息。
 * 现在改为：握手时用 JWT 校验身份并绑定到 Session，
 * 发送消息时以 Session 中绑定的身份为准，完全忽略客户端传入的发送者字段。
 */
@Slf4j
@Component
@ServerEndpoint("/ws/chat")
public class ChatWebSocket {

    private static ChatMessageService chatMessageService;
    private static JwtUtils jwtUtils;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 在线连接池: key = userId_role */
    private static final Map<String, Session> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    /** Session 中存放身份的 key */
    private static final String ATTR_USER_ID = "authUserId";
    private static final String ATTR_ROLE = "authRole";

    @Autowired
    public void setChatMessageService(ChatMessageService service) {
        ChatWebSocket.chatMessageService = service;
    }

    @Autowired
    public void setJwtUtils(JwtUtils utils) {
        ChatWebSocket.jwtUtils = utils;
    }

    /** 当前连接的 userId_role */
    private String sessionKey;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        String token = extractToken(session.getQueryString());
        if (token == null || jwtUtils == null || !jwtUtils.isValid(token)) {
            log.warn("WebSocket 连接鉴权失败，关闭连接");
            closeQuietly(session);
            return;
        }
        Long userId = jwtUtils.getUserId(token);
        Integer role = jwtUtils.getRole(token);
        if (userId == null || role == null) {
            log.warn("WebSocket token 中缺少用户信息，关闭连接");
            closeQuietly(session);
            return;
        }

        // 身份绑定到 Session，后续消息一律以此为准
        session.getUserProperties().put(ATTR_USER_ID, userId);
        session.getUserProperties().put(ATTR_ROLE, role);

        this.sessionKey = userId + "_" + role;
        ONLINE_SESSIONS.put(this.sessionKey, session);
        log.info("WebSocket 连接成功: {}", sessionKey);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            // 发送者身份取连接时鉴权的结果，不信任消息体中的 senderId / senderRole
            Object attrUserId = session.getUserProperties().get(ATTR_USER_ID);
            Object attrRole = session.getUserProperties().get(ATTR_ROLE);
            if (!(attrUserId instanceof Long) || !(attrRole instanceof Integer)) {
                log.warn("WebSocket 会话缺少身份信息，拒绝处理消息");
                closeQuietly(session);
                return;
            }
            Long senderId = (Long) attrUserId;
            Integer senderRole = (Integer) attrRole;

            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(message, Map.class);
            if (msg.get("receiverId") == null || msg.get("receiverRole") == null) {
                log.warn("WebSocket 消息缺少接收方信息");
                return;
            }
            Long receiverId = Long.valueOf(msg.get("receiverId").toString());
            Integer receiverRole = Integer.valueOf(msg.get("receiverRole").toString());
            String content = (String) msg.get("content");

            ChatMessage chatMsg = new ChatMessage();
            chatMsg.setSenderId(senderId);
            chatMsg.setSenderRole(senderRole);
            chatMsg.setReceiverId(receiverId);
            chatMsg.setReceiverRole(receiverRole);
            chatMsg.setContent(content);
            chatMsg.setIsRead(0);
            chatMessageService.save(chatMsg);

            Map<String, Object> pushMsg = new HashMap<>();
            pushMsg.put("id", chatMsg.getId());
            pushMsg.put("senderId", senderId);
            pushMsg.put("senderRole", senderRole);
            pushMsg.put("receiverId", receiverId);
            pushMsg.put("receiverRole", receiverRole);
            pushMsg.put("content", content);
            pushMsg.put("createTime", LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            String receiverKey = receiverId + "_" + receiverRole;
            Session receiverSession = ONLINE_SESSIONS.get(receiverKey);
            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.getAsyncRemote().sendText(objectMapper.writeValueAsString(pushMsg));
            }
        } catch (Exception e) {
            log.error("WebSocket消息处理失败", e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        if (sessionKey != null) {
            ONLINE_SESSIONS.remove(sessionKey);
            log.info("WebSocket断开: {}", sessionKey);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误: {}", sessionKey, error);
    }

    /** 从 query string 中解析 token 参数 */
    private String extractToken(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String param : query.split("&")) {
            int idx = param.indexOf('=');
            if (idx > 0 && "token".equals(param.substring(0, idx))) {
                return param.substring(idx + 1);
            }
        }
        return null;
    }

    private void closeQuietly(Session session) {
        try {
            session.close();
        } catch (IOException e) {
            // 连接可能已关闭，忽略
        }
    }
}

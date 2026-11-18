package com.mushroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置 (在线客服聊天)
 *
 * ChatWebSocket 类已通过 @Component 注册为 Spring Bean（bean 名 chatWebSocket），
 * 这里仅需注册 ServerEndpointExporter 让 @ServerEndpoint 注解生效，
 * 切勿再 @Bean ChatWebSocket，否则会因 bean 名冲突导致 Spring 启动失败。
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

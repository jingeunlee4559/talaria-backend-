package com.example.talaria.backend.config;

import com.example.talaria.backend.ws.SubtitleWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SubtitleWebSocketHandler handler;

    public WebSocketConfig(SubtitleWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/subtitles")
                .setAllowedOrigins("*");  // 프론트엔드 3000 포트 접근 허용
    }
}

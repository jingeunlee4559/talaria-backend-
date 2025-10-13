package com.example.talaria.backend.config;

import com.example.talaria.backend.ws.AuthHandshakeInterceptor;
import com.example.talaria.backend.ws.SubtitleWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SubtitleWebSocketHandler handler;
    private final AuthHandshakeInterceptor auth;

    public WebSocketConfig(SubtitleWebSocketHandler handler, AuthHandshakeInterceptor auth) {
        this.handler = handler; this.auth = auth;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/subtitles")
                .addInterceptors(auth)
                .setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000");
    }
}

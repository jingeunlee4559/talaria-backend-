package com.example.talaria.backend.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;

@Component
public class SubtitleWebSocketHandler implements WebSocketHandler {

    private final WebSocketChannelRegistry registry;
    private final ObjectMapper om = new ObjectMapper();

    public SubtitleWebSocketHandler(WebSocketChannelRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sid = (String) session.getAttributes().get("sid");
        registry.join(sid, session);

        // 입장 알림 (선택)
        var msg = Map.of("type", "notice", "text", "client joined", "ts", System.currentTimeMillis());
        session.sendMessage(new TextMessage(om.writeValueAsString(msg)));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sid = (String) session.getAttributes().get("sid");
        // 받은 메시지를 같은 sid 룸에 브로드캐스트
        for (var peer : registry.peers(sid)) {
            if (peer.isOpen()) {
                peer.sendMessage((TextMessage) message);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 로깅만
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sid = (String) session.getAttributes().get("sid");
        registry.leave(sid, session);
    }

    @Override
    public boolean supportsPartialMessages() { return false; }
}

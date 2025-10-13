package com.example.talaria.backend.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;

@Component
public class SubtitleWebSocketHandler implements WebSocketHandler {
    private final WebSocketChannelRegistry registry;
    private final ObjectMapper om = new ObjectMapper();

    public SubtitleWebSocketHandler(WebSocketChannelRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sid = (String) session.getAttributes().get("sid");
        if (sid == null) sid = "unknown";

        try {
            Map<String, Object> in = om.readValue(
                    message.getPayload().toString(),
                    new TypeReference<Map<String, Object>>() {}
            );

            String type = Optional.ofNullable(in.get("type"))
                    .map(Object::toString)
                    .orElse("partial");
            String text = Optional.ofNullable(in.get("text"))
                    .map(Object::toString)
                    .orElse("");

            registry.broadcast(sid, om.writeValueAsString(Map.of(
                    "type", type,
                    "text", text,
                    "ts", Instant.now().toEpochMilli()
            )));
        } catch (Exception e) {
            registry.broadcast(sid, om.writeValueAsString(Map.of(
                    "type", "error",
                    "message", "Invalid message format",
                    "detail", e.getMessage()
            )));
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sid = extractSessionId(session.getUri());
        session.getAttributes().put("sid", sid);
        registry.join(sid, session);
        registry.broadcast(sid, om.writeValueAsString(Map.of("type", "notice", "message", "client joined: " + sid)));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {}

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sid = (String) session.getAttributes().get("sid");
        registry.leave(sid, session);
        registry.broadcast(sid, om.writeValueAsString(Map.of("type", "notice", "message", "client left")));
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String extractSessionId(URI uri) {
        if (uri == null || uri.getQuery() == null) return "unknown";
        for (String p : uri.getQuery().split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && kv[0].equals("sid")) return kv[1];
        }
        return "unknown";
    }
}

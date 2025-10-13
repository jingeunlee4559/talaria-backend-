package com.example.talaria.backend.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketChannelRegistry {
    private final Map<String, Set<WebSocketSession>> channels = new ConcurrentHashMap<>();

    public void join(String sid, WebSocketSession session) {
        channels.computeIfAbsent(sid, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void leave(String sid, WebSocketSession session) {
        Set<WebSocketSession> set = channels.get(sid);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) channels.remove(sid);
        }
    }

    public void broadcast(String sid, String payload) {
        Set<WebSocketSession> set = channels.get(sid);
        if (set == null) return;
        for (WebSocketSession ws : set) {
            if (ws.isOpen()) {
                try { ws.sendMessage(new TextMessage(payload)); } catch (Exception ignored) {}
            }
        }
    }
}

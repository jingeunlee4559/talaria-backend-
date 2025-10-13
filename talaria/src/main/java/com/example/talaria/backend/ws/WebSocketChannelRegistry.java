package com.example.talaria.backend.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketChannelRegistry {
    // sid -> sessions
    private final ConcurrentHashMap<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public void join(String sid, WebSocketSession session) {
        rooms.computeIfAbsent(sid, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void leave(String sid, WebSocketSession session) {
        var set = rooms.get(sid);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) rooms.remove(sid);
        }
    }

    public Set<WebSocketSession> peers(String sid) {
        return rooms.getOrDefault(sid, ConcurrentHashMap.newKeySet());
    }
}

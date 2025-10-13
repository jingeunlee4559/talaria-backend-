package com.example.talaria.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    public record Session(String id, String token, long expiresAt, Map<String,Object> settings) {}

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom rnd = new SecureRandom();

    private final long ttlSec;

    public SessionService(@Value("${talaria.sessionTtlSeconds:600}") long ttlSec) {
        this.ttlSec = ttlSec;
    }

    public Session create() {
        String sid = hex(6);
        String token = hex(16);
        long exp = Instant.now().getEpochSecond() + ttlSec;
        Map<String,Object> settings = new HashMap<>(Map.of(
                "voiceover_enabled", true,
                "caption_commit_ms", 400,
                "font_size", 18,
                "high_contrast", false
        ));
        Session s = new Session(sid, token, exp, settings);
        sessions.put(sid, s);
        return s;
    }

    public Session get(String sid) { return sessions.get(sid); }

    public boolean end(String sid) { return sessions.remove(sid) != null; }

    public boolean updateSettings(String sid, Map<String,Object> patch) {
        Session s = sessions.get(sid);
        if (s == null) return false;
        s.settings().putAll(patch);
        return true;
    }

    public boolean validate(String sid, String token) {
        Session s = sessions.get(sid);
        return s != null && Objects.equals(s.token(), token) && s.expiresAt() > Instant.now().getEpochSecond();
    }

    @Scheduled(fixedDelay = 30_000)
    public void sweep() {
        long now = Instant.now().getEpochSecond();
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
    }

    private String hex(int bytes) {
        byte[] b = new byte[bytes];
        rnd.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}

package com.example.talaria.backend.controller;

import com.example.talaria.backend.model.SessionCreateResponse;
import com.example.talaria.backend.model.SessionSettingsRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final Map<String, Map<String, Object>> SESSIONS = new ConcurrentHashMap<>();
    private final SecureRandom rnd = new SecureRandom();

    @Value("${talaria.sessionTtlSeconds:600}")
    private long sessionTtl;

    @PostMapping
    public SessionCreateResponse create() {
        String sid = hex(6);
        long now = Instant.now().getEpochSecond();
        long exp = now + sessionTtl;
        String token = hex(16);

        Map<String, Object> settings = new HashMap<>();
        settings.put("voiceover_enabled", true);
        settings.put("caption_commit_ms", 400);
        settings.put("font_size", 18);
        settings.put("high_contrast", false);

        Map<String, Object> sess = new HashMap<>();
        sess.put("created_at", now);
        sess.put("expires_at", exp);
        sess.put("settings", settings);
        SESSIONS.put(sid, sess);

        return new SessionCreateResponse(
                sid,
                "/ws/subtitles?sid=" + sid,
                token,
                exp
        );
    }

    @PostMapping("/{sid}/end")
    public Map<String, Object> end(@PathVariable String sid) {
        boolean ok = (SESSIONS.remove(sid) != null);
        return Map.of("ok", ok, "session_id", sid);
    }

    @PostMapping("/{sid}/settings")
    public Map<String, Object> settings(@PathVariable String sid, @RequestBody SessionSettingsRequest req) {
        Map<String, Object> sess = SESSIONS.get(sid);
        if (sess == null) return Map.of("ok", false, "session_id", sid);
        Map<String, Object> st = (Map<String, Object>) sess.get("settings");
        if (req.voiceover_enabled() != null) st.put("voiceover_enabled", req.voiceover_enabled());
        if (req.caption_commit_ms() != null) st.put("caption_commit_ms", req.caption_commit_ms());
        if (req.font_size() != null) st.put("font_size", req.font_size());
        if (req.high_contrast() != null) st.put("high_contrast", req.high_contrast());
        return Map.of("ok", true, "session_id", sid);
    }

    private String hex(int bytes) {
        byte[] b = new byte[bytes];
        rnd.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte v : b) sb.append(String.format("%02x", v));
        return sb.toString();
    }
}

package com.example.talaria.backend.controller;

import com.example.talaria.backend.config.LivekitConfig;
import com.example.talaria.backend.service.LivekitService;
import com.example.talaria.backend.service.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
public class SessionController {

    private final SessionService sessionService;
    private final String wsBase;
    private final LivekitService livekitService;
    private final LivekitConfig livekitConfig;

    public SessionController(SessionService sessionService,
                             @Value("${server.port:8080}") int port,
                             LivekitService livekitService,
                             LivekitConfig livekitConfig) {
        this.sessionService = sessionService;
        this.wsBase = "ws://localhost:" + port;
        this.livekitService = livekitService;
        this.livekitConfig = livekitConfig;
    }

    @PostMapping("/sessions")
    public Map<String, Object> createSession() {
        var s = sessionService.create();
        String wsUrl = wsBase + "/ws/subtitles?sid=" + s.id() + "&token=" + s.token();
        String room = "talaria-" + s.id();
        String identity = "user-" + s.id();
        long now = Instant.now().getEpochSecond();
        long ttlSec = Math.max(60, s.expiresAt() - now);
        String lkToken = livekitService.createJoinToken(room, identity, "Guest", ttlSec);

        return Map.of(
            "session_id", s.id(),
            "token", s.token(),
            "expires_at", s.expiresAt(),
            "ws_url", wsUrl,
            "settings", s.settings(),
            "livekit", Map.of(
                "wsUrl", livekitConfig.getWsUrl(),
                "token", lkToken,
                "room", room,
                "identity", identity
            )
        );
    }

    @PostMapping("/sessions/{sid}/settings")
    public ResponseEntity<?> updateSettings(@PathVariable String sid, @RequestBody Map<String, Object> patch) {
        boolean ok = sessionService.updateSettings(sid, patch);
        return ok ? ResponseEntity.ok(Map.of("updated", true)) : ResponseEntity.notFound().build();
    }

    @PostMapping("/sessions/{sid}/end")
    public ResponseEntity<?> end(@PathVariable String sid) {
        boolean ok = sessionService.end(sid);
        return ok ? ResponseEntity.ok(Map.of("ended", true)) : ResponseEntity.notFound().build();
    }
}

package com.example.talaria.backend.controller;

import com.example.talaria.backend.config.LivekitConfig;
import com.example.talaria.backend.service.LivekitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
public class LivekitController {

  private final LivekitService service;
  private final LivekitConfig cfg;

  public record TokenReq(String room, String identity, String name, Long ttlSec) {}
  public record TokenRes(String wsUrl, String token, String room, String identity, long ttlSec) {}

  // 영문/숫자/하이픈/언더스코어만 허용 (LiveKit 권장 패턴 수준)
  private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

  @PostMapping("/token")
  public ResponseEntity<?> issue(@RequestBody TokenReq req) {
    // LiveKit 설정 확인
    if (isBlank(cfg.getWsUrl()) || isBlank(cfg.getApiUrl())
        || isBlank(cfg.getApiKey()) || isBlank(cfg.getApiSecret())) {
      return ResponseEntity.status(503)
          .body(Map.of("error", "livekit_disabled", "message", "LiveKit config is missing"));
    }

    // 필수값 검증
    if (req == null || isBlank(req.room()) || isBlank(req.identity())) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "invalid_args", "message", "room and identity are required"));
    }
    if (!VALID.matcher(req.room()).matches() || !VALID.matcher(req.identity()).matches()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "invalid_args",
                       "message", "room/identity must match [A-Za-z0-9_-], length 1..64"));
    }

    String room = req.room();
    String identity = req.identity();
    String name = (req.name() == null || req.name().isBlank()) ? "Guest" : trim(req.name(), 64);

    long ttl = (req.ttlSec() == null) ? 600L : req.ttlSec();
    if (ttl < 30L || ttl > 3600L) ttl = 600L; // 최소 30초, 최대 1시간 범위 내

    String jwt = service.createJoinToken(room, identity, name, ttl);

    return ResponseEntity.ok(new TokenRes(cfg.getWsUrl(), jwt, room, identity, ttl));
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
  private static String trim(String s, int max) {
    return (s.length() <= max) ? s : s.substring(0, max);
  }
}

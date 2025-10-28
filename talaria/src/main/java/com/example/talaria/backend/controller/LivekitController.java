package com.example.talaria.backend.controller;

import com.example.talaria.backend.config.LivekitConfig;
import com.example.talaria.backend.service.LivekitService;
import com.example.talaria.backend.service.meeting.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
public class LivekitController {

  private final LivekitService service;
  private final LivekitConfig cfg;
  private final MeetingService meetingService;

  // room: 선택(있으면 기존 회의 조회에만 사용, 생성에는 사용하지 않음)
  public record TokenReq(
      String room,
      String identity,
      String name,
      Long ttlSec,
      Long hostId,
      String title,
      String defaultLang
  ) {}

  public record TokenRes(
      String wsUrl,
      String token,
      String room,          // = 최종 사용되는 invite_code
      String identity,
      long ttlSec,
      Map<String,Object> meeting
  ) {}

  // identity만 강하게 검증. room은 선택값이므로 주면만 형식검증.
  private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

  @PostMapping("/token")
  public ResponseEntity<?> issue(@RequestBody TokenReq req) {
    // 0) LiveKit 설정 체크
    if (isBlank(cfg.getWsUrl()) || isBlank(cfg.getApiUrl())
        || isBlank(cfg.getApiKey()) || isBlank(cfg.getApiSecret())) {
      return ResponseEntity.status(503).body(Map.of(
          "error", "livekit_disabled",
          "message", "LiveKit config is missing"
      ));
    }

    // 1) 입력 검증
    if (req == null || isBlank(req.identity())) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_args",
          "message", "identity is required"
      ));
    }
    if (!VALID.matcher(req.identity()).matches()) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_args",
          "message", "identity must match [A-Za-z0-9_-], length 1..64"
      ));
    }
    if (req.hostId() == null || req.hostId() <= 0) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_args",
          "message", "hostId is required and must be > 0"
      ));
    }
    // room이 전달되면 형식만 확인(생성에는 사용하지 않음)
    if (!isBlank(req.room()) && !VALID.matcher(req.room()).matches()) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_args",
          "message", "room must match [A-Za-z0-9_-], length 1..64"
      ));
    }

    final long hostId = req.hostId();
    final String identity = req.identity();
    final String name = (req.name() == null || req.name().isBlank()) ? "Guest" : trim(req.name(), 64);
    final long ttl = normalizeTtl(req.ttlSec());

    final String title = isBlank(req.title()) ? "New Meeting" : trim(req.title(), 200);
    final String defaultLang = isBlank(req.defaultLang()) ? "ko" : trim(req.defaultLang(), 10);
    final LocalDateTime scheduledAt = LocalDateTime.now();

    Map<String,Object> meeting;

    try {
      // 2) room이 주어지면: 기존 회의 검색(있으면 그 회의로 진행)
      if (!isBlank(req.room())) {
        meeting = meetingService.getByInviteCode(req.room());
        if (meeting == null || meeting.isEmpty()) {
          // 없으면 새로 생성(여기서 invite_code는 서비스 내부 랜덤 생성)
          meeting = meetingService.createNew(hostId, title, defaultLang, scheduledAt);
        }
      } else {
        // 2’) room이 없으면 무조건 새로 생성(랜덤 invite_code)
        meeting = meetingService.createNew(hostId, title, defaultLang, scheduledAt);
      }
    } catch (Exception e) {
      var pd = ProblemDetail.forStatus(500);
      pd.setTitle("Database Error");
      pd.setDetail("Database error");
      pd.setInstance(URI.create("/livekit/token"));
      return ResponseEntity.status(500).body(Map.of(
          "type", "about:blank",
          "title", pd.getTitle(),
          "status", 500,
          "detail", pd.getDetail(),
          "instance", pd.getInstance().toString()
      ));
    }

    // 3) 최종 room = meeting.invite_code (백엔드 생성 or 기존 것)
    final String finalRoom = String.valueOf(meeting.get("invite_code"));

    // 4) LiveKit 토큰 발급(방 이름 = 최종 invite_code)
    String jwt = service.createJoinToken(finalRoom, identity, name, ttl);

    return ResponseEntity.ok(new TokenRes(
        cfg.getWsUrl(), jwt, finalRoom, identity, ttl, meeting
    ));
  }

  @PostMapping("/ai/stt/token")
  public ResponseEntity<?> issueSttBotToken(@RequestBody TokenReq req) {
    if (isBlank(cfg.getWsUrl()) || isBlank(cfg.getApiKey()) || isBlank(cfg.getApiSecret())) {
      return ResponseEntity.status(503).body(Map.of(
          "error", "livekit_disabled",
          "message", "LiveKit config is missing"
      ));
    }
    // room이 없으면 STT 봇도 새 방이 아닌, 별도 정책이 필요함.
    if (isBlank(req.room())) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_args",
          "message", "room is required for STT bot"
      ));
    }
    if (!VALID.matcher(req.room()).matches()) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_args",
          "message", "room must match [A-Za-z0-9_-], length 1..64"
      ));
    }

    final String room = req.room();
    String identity = ("stt-bot-" + room);
    if (identity.length() > 64) identity = identity.substring(0, 64);

    long ttl = normalizeTtl(req.ttlSec() == null ? 3600L : req.ttlSec());
    String token = service.createJoinToken(room, identity, "STT Bot", ttl);

    return ResponseEntity.ok(Map.of(
        "wsUrl", cfg.getWsUrl(),
        "token", token,
        "room", room,
        "identity", identity,
        "ttlSec", ttl
    ));
  }

  // ===== utils =====
  private static boolean isBlank(String s) { return s == null || s.isBlank(); }
  private static String trim(String s, int max) { return (s.length() <= max) ? s : s.substring(0, max); }
  private static long normalizeTtl(Long ttlSec) {
    long ttl = (ttlSec == null) ? 60L : ttlSec;
    if (ttl < 30L || ttl > 3600L) ttl = 60L;
    return ttl;
  }
}

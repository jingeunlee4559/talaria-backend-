package com.example.talaria.backend.controller;

import com.example.talaria.backend.config.LivekitConfig;
import com.example.talaria.backend.mapper.user.UserMapper;
import com.example.talaria.backend.service.LivekitAdminService;
import com.example.talaria.backend.service.LivekitService;
import com.example.talaria.backend.service.meeting.MeetingService;
import com.example.talaria.backend.service.meeting.ParticipantService;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
public class LivekitController {

  private final LivekitService service;
  private final LivekitConfig cfg;
  private final MeetingService meetingService;
  private final LivekitAdminService adminService;
  private final UserMapper userMapper; // 사용자 locale 조회용

  @Autowired(required = false)
  private ParticipantService participantService; // 선택 주입

  /** 프론트 바디 유지: meetingId 포함 */
  public record TokenReq(
      String room,
      Long meetingId,
      String identity,
      String name,
      @JsonProperty("ttlSec") @JsonAlias("ttl_sec") Long ttlSec,
      @JsonProperty("hostId") @JsonAlias("host_id") Long hostId,
      String title,
      String defaultLang,
      @JsonProperty("userId") @JsonAlias("user_id") Long userId
  ) {}

  private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

  @PostMapping("/token")
  public ResponseEntity<?> issue(@RequestBody TokenReq req) {
    // 0) LiveKit 설정 확인
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
    if (!isBlank(req.room()) && !VALID.matcher(req.room()).matches()) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "invalid_args",
          "message", "room must match [A-Za-z0-9_-], length 1..64"
      ));
    }

    long ttl = normalizeTtl(req.ttlSec());
    String identity = req.identity();
    String displayName = isBlank(req.name()) ? "Guest" : trim(req.name(), 64);
    // 2) 회의 선택/생성
    Map<String,Object> meeting;
    try {
      if (req.meetingId() != null && req.meetingId() > 0) {
        meeting = meetingService.getById(req.meetingId());
        if (meeting == null || meeting.isEmpty()) {
          return ResponseEntity.status(404).body(Map.of(
              "error", "meeting_not_found",
              "message", "meetingId not found"
          ));
        }
      } else if (!isBlank(req.room())) {
        meeting = meetingService.getByInviteCode(req.room());
        if (meeting == null || meeting.isEmpty()) {
          if (req.hostId() == null || req.hostId() <= 0) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "room_not_found",
                "message", "invite_code (room) not found"
            ));
          }
          meeting = meetingService.createNew(
              req.hostId(),
              isBlank(req.title()) ? "New Meeting" : trim(req.title(), 200),
              isBlank(req.defaultLang()) ? "ko" : trim(req.defaultLang(), 10),
              LocalDateTime.now()
          );
        }
      } else {
        if (req.hostId() == null || req.hostId() <= 0) {
          return ResponseEntity.badRequest().body(Map.of(
              "error", "invalid_args",
              "message", "hostId is required when room is not provided"
          ));
        }
        meeting = meetingService.createNew(
            req.hostId(),
            isBlank(req.title()) ? "New Meeting" : trim(req.title(), 200),
            isBlank(req.defaultLang()) ? "ko" : trim(req.defaultLang(), 10),
            LocalDateTime.now()
        );
      }
    } catch (DataAccessException dae) {
      var pd = ProblemDetail.forStatus(500);
      pd.setTitle("Database Error");
      pd.setDetail("Database error: " + safeMsg(dae.getMostSpecificCause()));
      pd.setInstance(URI.create("/livekit/token"));
      return ResponseEntity.status(500).body(Map.of(
          "type", "about:blank",
          "title", pd.getTitle(),
          "status", 500,
          "detail", pd.getDetail(),
          "instance", pd.getInstance().toString()
      ));
    } catch (Exception e) {
      var pd = ProblemDetail.forStatus(500);
      pd.setTitle("Server Error");
      pd.setDetail("Unexpected error: " + safeMsg(e));
      pd.setInstance(URI.create("/livekit/token"));
      return ResponseEntity.status(500).body(Map.of(
          "type", "about:blank",
          "title", pd.getTitle(),
          "status", 500,
          "detail", pd.getDetail(),
          "instance", pd.getInstance().toString()
      ));
    }

    // 3) locale 결정
    String finalRoom = String.valueOf(meeting.get("invite_code"));
    // 사용자 locale (tl_users.locale) 조회 (없으면 ko-KR)
    String userLocale = "ko-KR";
    try {
      Long uid = req.userId();
      if (uid != null && uid > 0) {
        String loc = userMapper.findLocaleById(uid);
        if (loc != null && !loc.isBlank()) {
          userLocale = toBcp47(loc); // 예: "ko" -> "ko-KR"
        }
      }
    } catch (Exception ignore) {
      // 조회 실패해도 기본값 유지
    }

    // 회의 기본 언어 → BCP47로 보정
    String targetLocale = toBcp47(String.valueOf(meeting.getOrDefault("default_lang", "ko")));

    // 4) LiveKit 토큰(메타데이터 포함) 발급
    long meetingsId = ((Number) meeting.get("meetings_id")).longValue();
    String jwt = service.createJoinTokenWithMeta(
    finalRoom, identity, displayName, ttl, userLocale, targetLocale, req.userId(), meetingsId
    );

    // 5) 참가자 JOIN 기록(실패 무시)
    if (participantService != null) {
      try {
        Long uid = req.userId();
        if (uid != null && uid > 0) {
          long hostId = meeting.get("host_id") == null ? -1L : ((Number) meeting.get("host_id")).longValue();
          String role = (uid == hostId) ? "HOST" : "GUEST";
          participantService.joined(meetingsId, uid, role);
        }
      } catch (Exception joinEx) {
        System.err.println("[participant.joined] non-fatal error: " + safeMsg(joinEx));
      }
    }

    // 6) A안: Map 응답 + meeting_id(snake_case) 포함
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("wsUrl", cfg.getWsUrl());
    body.put("token", jwt);
    body.put("room", finalRoom);
    body.put("identity", identity);
    body.put("ttlSec", ttl);
    body.put("userLocale", userLocale);
    body.put("meeting_id", meetingsId); // ← snake_case 추가
    body.put("meeting", meeting);       // (호환 유지를 원하면 포함)

    return ResponseEntity.ok(body);
  }

  @PostMapping("/ai/stt/token")
  public ResponseEntity<?> issueSttBotToken(@RequestBody TokenReq req) {
    if (isBlank(cfg.getWsUrl()) || isBlank(cfg.getApiKey()) || isBlank(cfg.getApiSecret())) {
      return ResponseEntity.status(503).body(Map.of(
          "error", "livekit_disabled",
          "message", "LiveKit config is missing"
      ));
    }
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

    String room = req.room();
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

  @GetMapping("/meetings/{id}/participants/count")
  public ResponseEntity<?> count(@PathVariable long id) {
    var meeting = meetingService.getById(id);
    if (meeting == null) return ResponseEntity.notFound().build();

    String room = String.valueOf(meeting.get("invite_code"));
    int count = adminService.countParticipantsByRoom(room);
    return ResponseEntity.ok(Map.of("room", room, "count", count));
  }

  // ===== utils =====
  private static boolean isBlank(String s) { return s == null || s.isBlank(); }
  private static String trim(String s, int max) { return (s.length() <= max) ? s : s.substring(0, max); }
  private static long normalizeTtl(Long ttlSec) {
    long ttl = (ttlSec == null) ? 60L : ttlSec;
    if (ttl < 30L || ttl > 3600L) ttl = 60L;
    return ttl;
  }
  private static String safeMsg(Throwable t) {
    if (t == null) return "";
    String m = t.getMessage();
    return (m == null) ? t.getClass().getSimpleName() : m;
  }

  /** "ko" → "ko-KR", "en" → "en-US" 등 간단 매핑 */
  private static String toBcp47(String val) {
    if (val == null || val.isBlank()) return "ko-KR";
    String v = val.replace('_','-').toLowerCase(Locale.ROOT);
    if (v.equals("ko") || v.equals("ko-kr")) return "ko-KR";
    if (v.equals("en") || v.equals("en-us")) return "en-US";
    if (v.equals("ja") || v.equals("ja-jp")) return "ja-JP";
    if (v.equals("zh") || v.equals("zh-cn")) return "zh-CN";
    if (v.equals("pt") || v.equals("pt-br")) return "pt-BR";
    if (v.equals("ar") || v.equals("ar-sa")) return "ar-SA";
    return v.substring(0,1).toLowerCase(Locale.ROOT) + v.substring(1);
  }
}

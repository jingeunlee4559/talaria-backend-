// LivekitController.java
package com.example.talaria.backend.controller;

import com.example.talaria.backend.config.LivekitConfig;
import com.example.talaria.backend.service.LivekitAdminService;
import com.example.talaria.backend.service.LivekitService;
import com.example.talaria.backend.service.meeting.MeetingService;
import com.example.talaria.backend.service.meeting.ParticipantService;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;              // ⬅️ 추가
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

// ⬅️ (선택) 사용자 조회 서비스가 있다면 주입
import com.example.talaria.backend.service.user.UserService; // 없으면 본인 프로젝트 패키지 맞춰주세요

@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
public class LivekitController {

  private final LivekitService service;
  private final LivekitConfig cfg;
  private final MeetingService meetingService;
  private final LivekitAdminService adminService;

  @Autowired(required = false)
  private ParticipantService participantService;

  @Autowired(required = false)
  private UserService userService;                           // ⬅️ 추가(선택)

  public record TokenReq(
      String room,
      String identity,
      String name,
      @JsonProperty("ttlSec") @JsonAlias("ttl_sec") Long ttlSec,
      @JsonProperty("hostId") @JsonAlias("host_id") Long hostId,
      String title,
      String defaultLang,
      @JsonProperty("userId") @JsonAlias("user_id") Long userId
  ) {}

  // ⬅️ userLocale 필드 추가
  public record TokenRes(
      String wsUrl,
      String token,
      String room,
      String identity,
      long ttlSec,
      Map<String,Object> meeting,
      String userLocale
  ) {}

  private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

  @PostMapping("/token")
  public ResponseEntity<?> issue(@RequestBody TokenReq req, HttpServletRequest httpReq) {
    if (isBlank(cfg.getWsUrl()) || isBlank(cfg.getApiUrl())
        || isBlank(cfg.getApiKey()) || isBlank(cfg.getApiSecret())) {
      return ResponseEntity.status(503).body(Map.of(
          "error", "livekit_disabled",
          "message", "LiveKit config is missing"
      ));
    }

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

    // 1) 회의 조회/생성
    Map<String,Object> meeting;
    try {
      if (!isBlank(req.room())) {
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

    String finalRoom = String.valueOf(meeting.get("invite_code"));

    // 2) 사용자 locale 결정: users.locale → 없으면 Accept-Language
    String userLocale = resolveUserLocale(req.userId());
    if (isBlank(userLocale)) {
      userLocale = bestAppLocale(httpReq); // Accept-Language
    }
    userLocale = toBcp47(userLocale); // ko → ko-KR 등으로 보정

    // 3) targetLocale(목표 로케일) = meeting.default_lang 기반
    String targetLocale = toBcp47FromDefaultLang(meeting.get("default_lang"));

    // 4) JWT metadata 구성
    Map<String,Object> metadata = new LinkedHashMap<>();
    metadata.put("appLocale", userLocale);      // 사용자의 앱 로케일
    metadata.put("userLocale", userLocale);     // 명시적으로도 보관
    metadata.put("targetLocale", targetLocale); // 회의 기본 언어
    metadata.put("username", displayName);
    metadata.put("userId", req.userId());
    metadata.put("identity", identity);

    // 5) LiveKit 토큰 발급 (메타데이터 포함)
    String jwt = service.createJoinTokenWithMetadata(finalRoom, identity, displayName, ttl, metadata);

    // 6) 참가자 기록(선택)
    if (participantService != null) {
      Long uid = req.userId();
      if (uid != null && uid > 0) {
        long meetingsId = ((Number) meeting.get("meetings_id")).longValue();
        long hostId = meeting.get("host_id") == null ? -1L : ((Number) meeting.get("host_id")).longValue();
        String role = (uid == hostId) ? "HOST" : "GUEST";
        participantService.joined(meetingsId, uid, role);
      }
    }

    // 7) 응답에 userLocale 포함
    return ResponseEntity.ok(new TokenRes(
        cfg.getWsUrl(), jwt, finalRoom, identity, ttl, meeting, userLocale
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

  /** DB의 users.locale 조회 (없으면 null) */
  private String resolveUserLocale(Long userId) {
    try {
      if (userId == null || userId <= 0 || userService == null) return null;
      Map<String,Object> u = userService.getById(userId); // ← 프로젝트에 맞게 구현되어야 함
      if (u == null) return null;
      Object loc = u.get("locale");
      String s = (loc == null) ? null : String.valueOf(loc).trim();
      return isBlank(s) ? null : s;
    } catch (Exception ignore) {
      return null;
    }
  }

  /** Accept-Language → BCP47 */
  private static String bestAppLocale(HttpServletRequest req) {
    try {
      var loc = req.getLocale();
      if (loc != null) return loc.toLanguageTag(); // ex) "ko-KR"
    } catch (Exception ignore) {}
    return "ko-KR";
  }

  /** "ko" → "ko-KR", "en" → "en-US" 등 */
  private static String toBcp47(String code) {
    if (isBlank(code)) return "ko-KR";
    String s = code.trim();
    if (s.contains("-")) return s; // 이미 BCP47
    s = s.toLowerCase(Locale.ROOT);
    return switch (s) {
      case "ko" -> "ko-KR";
      case "en" -> "en-US";
      case "zh" -> "zh-CN";
      case "ja" -> "ja-JP";
      case "ar" -> "ar-SA";
      case "pt" -> "pt-BR";
      default -> s; 
    };
  }

  /** meeting.default_lang → BCP47 */
  private static String toBcp47FromDefaultLang(Object val) {
    String s = String.valueOf(val == null ? "" : val).trim().toLowerCase(Locale.ROOT);
    if (s.isEmpty()) return "ko-KR";
    if (s.contains("-")) return s; // 이미 BCP47
    return toBcp47(s);
  }
}

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
  private final LivekitAdminService adminService;

  @Autowired(required = false)
  private ParticipantService participantService; // 선택 주입

  /**
   * 요청 DTO
   * - room: 선택. 있으면 그 invite_code로 입장 시도. 없으면 새 회의 생성(호스트만).
   * - hostId: 새 회의 생성 시 필수(기존 회의 입장만 할 땐 불필요).
   * - userId: 참가자 테이블 기록용(선택).
   */
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

  public record TokenRes(
      String wsUrl,
      String token,
      String room,          // 최종 invite_code
      String identity,
      long ttlSec,
      Map<String,Object> meeting
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
      if (!isBlank(req.room())) {
        // (A) 주어진 room(invite_code)로 조회
        meeting = meetingService.getByInviteCode(req.room());
        if (meeting == null || meeting.isEmpty()) {
          // 없고 hostId도 없으면 404
          if (req.hostId() == null || req.hostId() <= 0) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "room_not_found",
                "message", "invite_code (room) not found"
            ));
          }
          // hostId가 있으면 새로 생성
          meeting = meetingService.createNew(
              req.hostId(),
              isBlank(req.title()) ? "New Meeting" : trim(req.title(), 200),
              isBlank(req.defaultLang()) ? "ko" : trim(req.defaultLang(), 10),
              LocalDateTime.now()
          );
        }
      } else {
        // (B) room이 없으면 새 회의 생성(호스트만)
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

    // 3) 최종 room = meeting.invite_code
    String finalRoom = String.valueOf(meeting.get("invite_code"));

    // 4) LiveKit 토큰 발급
    String jwt = service.createJoinToken(finalRoom, identity, displayName, ttl);

    // 5) 참가자 JOIN 기록(선택) — ParticipantService가 존재하고 userId가 있으면 기록
    if (participantService != null) {
      Long uid = req.userId();
      if (uid != null && uid > 0) {
        long meetingsId = ((Number) meeting.get("meetings_id")).longValue();
        long hostId = meeting.get("host_id") == null ? -1L : ((Number) meeting.get("host_id")).longValue();
        String role = (uid == hostId) ? "HOST" : "GUEST";
        participantService.joined(meetingsId, uid, role); // joined_at 기록
      }
    }

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

  /**
   *  meetings_id로 현재 LiveKit 참가자 수 조회
   * GET /livekit/meetings/{meetingsId}/participants/count
   */
  // LivekitController.java (일부)
  @GetMapping("/meetings/{id}/participants/count")
  public ResponseEntity<?> count(@PathVariable long id) {
    var meeting = meetingService.getById(id);      // getById는 앞서 드린 대로 추가
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
}

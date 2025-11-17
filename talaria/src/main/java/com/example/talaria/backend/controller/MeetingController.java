package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.meeting.MeetingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings")
public class MeetingController {

    private final MeetingService service;

    // 목록 조회
    @GetMapping
    public ResponseEntity<PageResponse> list(
            @PathVariable("userId") long userId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "page", defaultValue = "0")  int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listByHost(userId, status, from, to, page, size));
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<Map<String,Object>> getOne(
            @PathVariable("userId") long userId,
            @PathVariable("id") long id) {
        var row = service.getOneByHost(userId, id);
        return row == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(row);
    }

    // 초대코드로 조회 (호스트 제약 없음)
    @GetMapping("/by-invite/{code}")
    public ResponseEntity<Map<String,Object>> getByInvite(@PathVariable("code") String code) {
        var row = service.getByInviteCode(code);
        return row == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(row);
    }

    // 생성 (scheduledAt이 null이면 now()로 보정)  — C모드 & 썸네일 반영
    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable("userId") long userId,
            @RequestBody @Valid CreateMeetingReq req) {
        try {
            var created = service.create(
                    userId,
                    req.getTitle(),
                    req.getDefaultLang(),
                    req.getScheduledAt(),
                    normalizeMode(req.getMeetingMode()),              // 'B' or 'C'
                    emptyToNull(req.getCultureThumbRelPath())        // null 허용
            );
            return ResponseEntity.ok(created);
        } catch (IllegalStateException e) {
            var pd = ProblemDetail.forStatus(409);
            pd.setTitle("Conflict");
            pd.setDetail(e.getMessage());
            pd.setInstance(URI.create("/api/users/" + userId + "/meetings"));
            return ResponseEntity.status(409).body(Map.of(
                    "type", "about:blank",
                    "title", pd.getTitle(),
                    "status", 409,
                    "detail", pd.getDetail(),
                    "instance", pd.getInstance().toString()
            ));
        }
    }

    // 부분 수정(제목/언어/시간/상태 등)
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String,Object>> patch(
            @PathVariable("userId") long userId,
            @PathVariable("id") long id,
            @RequestBody PatchMeetingReq req) {
        int n = service.updatePartial(userId, id,
                req.getTitle(), req.getStatus(), req.getInviteCode(),
                req.getDefaultLang(), req.getScheduledAt(), req.getEndedAt());
        return ResponseEntity.ok(Map.of("updated", n));
    }

    // 시작(STATUS: SCHEDULED -> ONGOING)
    @PatchMapping("/{id}/start")
    public ResponseEntity<Map<String,Object>> start(
            @PathVariable("userId") long userId, @PathVariable("id") long id) {
        int n = service.start(userId, id);
        return ResponseEntity.ok(Map.of("updated", n));
    }

    // 종료(STATUS: * -> ENDED, ended_at 세팅)
    @PatchMapping("/{id}/end")
    public ResponseEntity<Map<String,Object>> end(
            @PathVariable("userId") long userId, @PathVariable("id") long id) {
        int n = service.end(userId, id);
        return ResponseEntity.ok(Map.of("updated", n));
    }

    // 초대코드 재발급
    @PostMapping("/{id}/invite/regenerate")
    public ResponseEntity<Map<String,Object>> regenerateInvite(
            @PathVariable("userId") long userId, @PathVariable("id") long id) {
        int n = service.regenerateInvite(userId, id);
        return ResponseEntity.ok(Map.of("updated", n));
    }

    // 내가 호스트이거나 참가자인 모든 회의 조회
    @GetMapping("/all")
    public ResponseEntity<PageResponse> listAll(
        @PathVariable("userId") long userId,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(name = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(name = "page", defaultValue = "0")  int page,
        @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(
            service.listByUserIncludingParticipant(userId, status, from, to, page, size)
        );
    }

    // ===== DTO =====
    @Data
    public static class CreateMeetingReq {
        @NotBlank @Size(max = 200)
        private String title;
        @Size(max = 10)
        private String defaultLang = "ko";
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime scheduledAt;

        @Pattern(regexp = "^[BC]$", message = "meetingMode must be 'B' or 'C'")
        private String meetingMode;
        @Size(max = 512)
        private String cultureThumbRelPath;
    }

    @Data
    public static class PatchMeetingReq {
        @Size(max = 200)
        private String title;
        private String status; // 'SCHEDULED' | 'ONGOING' | 'ENDED'
        @Size(max = 255)
        private String inviteCode;
        @Size(max = 10)
        private String defaultLang;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime scheduledAt;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endedAt;
    }

    // ===== helpers =====
    private static String normalizeMode(String m) {
        if (m == null) return "B";
        String u = m.trim().toUpperCase();
        return (u.equals("C")) ? "C" : "B";
    }
    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}

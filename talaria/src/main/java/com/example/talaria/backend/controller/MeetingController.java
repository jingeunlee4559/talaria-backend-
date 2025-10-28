// src/main/java/com/example/talaria/backend/controller/MeetingController.java
package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.meeting.MeetingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    // 생성
    @PostMapping
public ResponseEntity<?> create(
        @PathVariable("userId") long userId,
        @RequestBody @Valid CreateMeetingReq req) {
    try {
        // DTO에서 defaultLang/scheduledAt 보정(빈값/널 방지)
        String defaultLang = (req.getDefaultLang() == null || req.getDefaultLang().isBlank())
                ? "ko" : req.getDefaultLang();
        var when = (req.getScheduledAt() != null)
                ? req.getScheduledAt()
                : LocalDateTime.now(); // 혹시 모를 누락 방지

        var created = service.createNew(userId, req.getTitle(), defaultLang, when);
        return ResponseEntity.ok(created);

    } catch (org.springframework.dao.DataIntegrityViolationException e) {
        // FK(host_id), UNIQUE 등 제약 위반을 400으로 명시
        var pd = ProblemDetail.forStatus(400);
        pd.setTitle("Bad Request");
        pd.setDetail("Invalid host_id (user not found) or constraint violation.");
        pd.setInstance(URI.create("/api/users/" + userId + "/meetings"));
        return ResponseEntity.status(400).body(Map.of(
                "type", "about:blank",
                "title", pd.getTitle(),
                "status", 400,
                "detail", pd.getDetail(),
                "instance", pd.getInstance().toString()
        ));

    } catch (IllegalStateException e) {
        // 초대코드 생성 충돌 재시도 실패 등
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

    // ===== DTO =====
    @Data
    public static class CreateMeetingReq {
        @NotBlank @Size(max = 200)
        private String title;

        @Size(max = 10)
        private String defaultLang = "ko";

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime scheduledAt;
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
}

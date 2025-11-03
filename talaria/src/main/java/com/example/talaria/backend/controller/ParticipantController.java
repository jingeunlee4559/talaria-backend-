package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.meeting.ParticipantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingsId}/participants")
public class ParticipantController {

    private final ParticipantService service;

    // 목록
    @GetMapping
    public ResponseEntity<PageResponse> list(@PathVariable long meetingsId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.list(meetingsId, page, size));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCount(@PathVariable long meetingsId) {
        long count = service.getTotalCount(meetingsId);
        return ResponseEntity.ok(Map.of("totalCount", count));
    }

    // 단건
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String,Object>> getOne(@PathVariable long meetingsId,
                                                     @PathVariable long userId) {
        var row = service.getOne(meetingsId, userId);
        return row == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(row);
    }

    // 초대 (role만 upsert)
    @PostMapping("/invite")
    public ResponseEntity<?> invite(@PathVariable long meetingsId,
                                    @RequestBody @Valid InviteReq req) {
        try {
            var row = service.invite(meetingsId, req.getUserId(), req.getRole());
            return ResponseEntity.ok(Map.of("participant", row));
        } catch (Exception e) {
            var pd = ProblemDetail.forStatus(500);
            pd.setTitle("Database Error");
            pd.setDetail("Database error");
            pd.setInstance(URI.create("/api/meetings/" + meetingsId + "/participants/invite"));
            return ResponseEntity.status(500).body(Map.of(
                "type", "about:blank",
                "title", pd.getTitle(),
                "status", 500,
                "detail", pd.getDetail(),
                "instance", pd.getInstance().toString()
            ));
        }
    }

    // 입장 (joined_at=NOW, left_at=NULL)
    @PostMapping("/join")
    public ResponseEntity<?> join(@PathVariable long meetingsId,
                                  @RequestBody @Valid JoinReq req) {
        try {
            var row = service.joined(meetingsId, req.getUserId(), req.getRole());
            return ResponseEntity.ok(Map.of("participant", row));
        } catch (Exception e) {
            var pd = ProblemDetail.forStatus(500);
            pd.setTitle("Database Error");
            pd.setDetail("Database error");
            pd.setInstance(URI.create("/api/meetings/" + meetingsId + "/participants/join"));
            return ResponseEntity.status(500).body(Map.of(
                "type", "about:blank",
                "title", pd.getTitle(),
                "status", 500,
                "detail", pd.getDetail(),
                "instance", pd.getInstance().toString()
            ));
        }
    }

    // 퇴장 (left_at=NOW)
    @PostMapping("/leave")
    public ResponseEntity<?> leave(@PathVariable long meetingsId,
                                   @RequestBody @Valid LeaveReq req) {
        try {
            var row = service.left(meetingsId, req.getUserId());
            return ResponseEntity.ok(Map.of("participant", row));
        } catch (Exception e) {
            var pd = ProblemDetail.forStatus(500);
            pd.setTitle("Database Error");
            pd.setDetail("Database error");
            pd.setInstance(URI.create("/api/meetings/" + meetingsId + "/participants/leave"));
            return ResponseEntity.status(500).body(Map.of(
                "type", "about:blank",
                "title", pd.getTitle(),
                "status", 500,
                "detail", pd.getDetail(),
                "instance", pd.getInstance().toString()
            ));
        }
    }

    // ===== DTOs =====
    @Data
    public static class InviteReq {
        @NotNull @Positive
        private Long userId;
        @Pattern(regexp="^(HOST|GUEST|VIEWER)$")
        private String role = "GUEST";
    }

    @Data
    public static class JoinReq {
        @NotNull @Positive
        private Long userId;
        // join 시 역할 지정이 필요없으면 생략 가능
        @Pattern(regexp="^(HOST|GUEST|VIEWER)$")
        private String role = "GUEST";
    }

    @Data
    public static class LeaveReq {
        @NotNull @Positive
        private Long userId;
    }
}

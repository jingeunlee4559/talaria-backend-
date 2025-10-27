package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.alert.AlertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/alerts")
public class AlertController {

    private final AlertService service;

    @GetMapping
    public ResponseEntity<?> list(
            @PathVariable("userId") long userId,
            @RequestParam(name = "alertId", required = false) String alertId,
            @RequestParam(name = "status",  required = false) String status,   // PENDING/SENT/SEEN
            @RequestParam(name = "page",    defaultValue = "0")  int page,
            @RequestParam(name = "size",    defaultValue = "20") int size) {

        // 1) alertId 숫자 검증 (잘못되면 400)
        Long idNullable = null;
        if (alertId != null && !alertId.isBlank()) {
            try {
                idNullable = Long.parseLong(alertId.trim());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "type","about:blank","title","Bad Request","status",400,
                        "detail","alertId must be a number"
                ));
            }
        }

        // 2) status 표준화 및 검증 (잘못되면 400)
        String normStatus = null;
        if (status != null && !status.isBlank()) {
            String u = status.trim().toUpperCase();
            if (!u.equals("PENDING") && !u.equals("SENT") && !u.equals("SEEN")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "type","about:blank","title","Bad Request","status",400,
                        "detail","status must be one of PENDING, SENT, SEEN"
                ));
            }
            normStatus = u;
        }

        // 3) 페이지 입력 방어
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        PageResponse body = service.listByUser(userId, idNullable, normStatus, page, size);
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<Map<String,Object>> create(
            @PathVariable("userId") long userId,
            @RequestBody @Valid CreateAlertReq req) {

        Map<String,Object> created = service.create(
                userId, req.getType(), req.getTitle(), req.getBody(), req.getPayloadJson());

        // id가 있으면 201 Created + Location 헤더 제공
        Object idObj = created.get("id");
        if (idObj != null) {
            return ResponseEntity
                    .created(URI.create(String.format("/api/users/%d/alerts/%s", userId, idObj)))
                    .body(created);
        }
        // (서비스가 id를 반환하지 않는 경우) 200
        return ResponseEntity.ok(created);
    }

    @PatchMapping("/{id}/delivered")
    public ResponseEntity<Map<String,Object>> delivered(
            @PathVariable("userId") long userId,
            @PathVariable("id") long id) {
        int n = service.markDelivered(userId, id);
        // idempotent 유지: 0이면 "이미 SENT거나 조건불일치"일 수 있음 → 200으로 통일
        return ResponseEntity.ok(Map.of("updated", n));
    }

    @PatchMapping("/{id}/seen")
    public ResponseEntity<Map<String,Object>> seen(
            @PathVariable("userId") long userId,
            @PathVariable("id") long id) {
        int n = service.markSeen(userId, id);
        // idempotent 유지: 0이면 "이미 SEEN/존재X/타유저" 케이스 → 200으로 통일
        return ResponseEntity.ok(Map.of("updated", n));
    }

    @PatchMapping("/seen")
    public ResponseEntity<Map<String,Object>> seenAll(
            @PathVariable("userId") long userId) {
        int n = service.markAllSeen(userId);
        return ResponseEntity.ok(Map.of("updated", n));
    }

    @Data
    public static class CreateAlertReq {
        @NotBlank private String type;     // "SYSTEM"/"MEETING"/"DOC" 등
        private String title;              // nullable
        private String body;               // nullable
        private String payloadJson;        // nullable, 유효 JSON 문자열
    }
}

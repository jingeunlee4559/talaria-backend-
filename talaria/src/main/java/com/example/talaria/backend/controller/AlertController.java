package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.alert.AlertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/alerts")
public class AlertController {

    private final AlertService service;

    @GetMapping
    public ResponseEntity<PageResponse> list(
            @PathVariable("userId") long userId,
            @RequestParam(name = "alertId", required = false) String alertId,
            @RequestParam(name = "status",  required = false) String status,   // PENDING/SENT/SEEN
            @RequestParam(name = "page",    defaultValue = "0")  int page,
            @RequestParam(name = "size",    defaultValue = "20") int size) {

        Long idNullable = (alertId == null || alertId.isBlank()) ? null : Long.parseLong(alertId);
        return ResponseEntity.ok(service.listByUser(userId, idNullable, status, page, size));
    }

    @PostMapping
    public ResponseEntity<Map<String,Object>> create(
            @PathVariable("userId") long userId,
            @RequestBody @Valid CreateAlertReq req) {

        return ResponseEntity.ok(
            service.create(userId, req.getType(), req.getTitle(), req.getBody(), req.getPayloadJson()));
    }

    @PatchMapping("/{id}/delivered")
    public ResponseEntity<Map<String,Object>> delivered(
            @PathVariable("userId") long userId,
            @PathVariable("id") long id) {

        int n = service.markDelivered(userId, id);
        return ResponseEntity.ok(Map.of("updated", n));
    }

    @PatchMapping("/{id}/seen")
    public ResponseEntity<Map<String,Object>> seen(
            @PathVariable("userId") long userId,
            @PathVariable("id") long id) {

        int n = service.markSeen(userId, id);
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

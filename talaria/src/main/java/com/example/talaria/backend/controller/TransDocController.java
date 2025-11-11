package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.transdoc.TransDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}")
public class TransDocController {

    private final TransDocService service;

    // ===== 기존: 문서별 목록 =====
    @GetMapping("/documents/{docId}/trans-docs")
    public ResponseEntity<PageResponse> listByDoc(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId,
            @RequestParam(name = "targetLang", required = false) String targetLang,
            @RequestParam(name = "provider", required = false) String provider,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listByDoc(userId, docId, targetLang, provider, page, size));
    }

    // ===== 단건: requestId =====
    @GetMapping("/trans-docs/req/{requestId}")
    public ResponseEntity<Map<String, Object>> getByRequestId(
            @PathVariable("userId") long userId,
            @PathVariable("requestId") String requestId
    ) {
        return ResponseEntity.ok(service.getOneByRequestId(userId, requestId));
    }

    // ===== 신규: requestId 생략 = 사용자 전체 조회 =====
    // 예: GET /api/users/1/trans-docs/req?targetLang=ja&provider=papago&page=0&size=20
    @GetMapping("/trans-docs/req")
    public ResponseEntity<PageResponse> listAllForUser(
            @PathVariable("userId") long userId,
            @RequestParam(name = "targetLang", required = false) String targetLang,
            @RequestParam(name = "provider", required = false) String provider,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listByUser(userId, targetLang, provider, page, size));
    }

    // ===== 최신 1건: (docId, targetLang[, provider]) =====
    @GetMapping("/documents/{docId}/trans-docs/lang/{targetLang}")
    public ResponseEntity<Map<String, Object>> getLatestByComposite(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId,
            @PathVariable("targetLang") String targetLang,
            @RequestParam(name = "provider", required = false) String provider
    ) {
        return ResponseEntity.ok(service.getLatestByComposite(userId, docId, targetLang, provider));
    }

    // ===== 삭제: requestId =====
    @DeleteMapping("/trans-docs/req/{requestId}")
    public ResponseEntity<Map<String, Object>> deleteByRequestId(
            @PathVariable("userId") long userId,
            @PathVariable("requestId") String requestId
    ) {
        int deleted = service.deleteByRequestId(userId, requestId);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    // ===== 삭제: (docId, targetLang[, provider]) =====
    @DeleteMapping("/documents/{docId}/trans-docs/lang/{targetLang}")
    public ResponseEntity<Map<String, Object>> deleteByComposite(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId,
            @PathVariable("targetLang") String targetLang,
            @RequestParam(name = "provider", required = false) String provider
    ) {
        int deleted = service.deleteByComposite(userId, docId, targetLang, provider);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}

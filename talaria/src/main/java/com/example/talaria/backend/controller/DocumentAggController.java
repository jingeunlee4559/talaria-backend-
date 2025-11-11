package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.document.DocumentAggService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/documents")
public class DocumentAggController {

    private final DocumentAggService service;

    // ===== Flat: 문서×번역 조인 결과(행 단위) =====
    // 예) GET /api/users/1/documents/with-trans?page=0&size=50&meetingId=10&targetLang=ja&provider=papago
    @GetMapping("/with-trans")
    public ResponseEntity<PageResponse> listCombinedFlat(
            @PathVariable("userId") long userId,
            @RequestParam(name = "meetingId", required = false) Long meetingId,
            @RequestParam(name = "docId", required = false) Long docId,
            @RequestParam(name = "targetLang", required = false) String targetLang,
            @RequestParam(name = "provider", required = false) String provider,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.listCombinedFlat(userId, meetingId, docId, targetLang, provider, page, size));
    }

    // ===== Grouped: 문서 레벨 페이징 + 번역 배열 =====
    // 예) GET /api/users/1/documents/with-trans/grouped?page=0&size=20&meetingId=10
    @GetMapping("/with-trans/grouped")
    public ResponseEntity<PageResponse> listCombinedGrouped(
            @PathVariable("userId") long userId,
            @RequestParam(name = "meetingId", required = false) Long meetingId,
            @RequestParam(name = "docId", required = false) Long docId,
            @RequestParam(name = "targetLang", required = false) String targetLang,
            @RequestParam(name = "provider", required = false) String provider,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                service.listCombinedGrouped(userId, meetingId, docId, targetLang, provider, page, size));
    }
}

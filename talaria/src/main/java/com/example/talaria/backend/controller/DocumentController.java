package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.document.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/documents")
public class DocumentController {

    private final DocumentService service;

    @GetMapping
    public ResponseEntity<PageResponse> list(
            @PathVariable("userId") long userId,
            @RequestParam(name = "meetingId", required = false) Long meetingId,
            @RequestParam(name = "page", defaultValue = "0")  int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listByUser(userId, meetingId, page, size));
    }

    @GetMapping("/{docId}")
    public ResponseEntity<Map<String,Object>> getOne(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId) {
        return ResponseEntity.ok(service.getOneByUser(userId, docId));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable("userId") long userId,
            @RequestBody @Valid CreateDocumentReq req) {
        try {
            var created = service.create(
                    userId,
                    req.getMeetingId(),
                    req.getOriginalName(),
                    req.getFilepath(),
                    req.getMimeType(),
                    req.getSize(),
                    req.getSha256(),
                    req.getOriginalLang(),
                    req.getStorageKey()
            );
            return ResponseEntity.ok(created);
        } catch (IllegalStateException ex) {
            // UNIQUE 위반 등 → 409 Conflict로 변환
            var pd = ProblemDetail.forStatus(409);
            pd.setTitle("Conflict");
            pd.setDetail("Duplicate filepath or constraint violation");
            pd.setInstance(URI.create("/api/users/" + userId + "/documents"));
            return ResponseEntity.status(409).body(Map.of(
                    "type","about:blank","title",pd.getTitle(),"status",409,"detail",pd.getDetail(),"instance",pd.getInstance().toString()
            ));
        }
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<Map<String,Object>> delete(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId) {
        int n = service.delete(userId, docId);
        return ResponseEntity.ok(Map.of("deleted", n));
    }

    // ===== DTO =====
    @Data
    public static class CreateDocumentReq {
        private Long meetingId; // nullable

        @NotBlank @Size(max = 255)
        private String originalName;

        @NotBlank @Size(max = 255)
        private String filepath;     // UNIQUE

        @NotBlank @Size(max = 100)
        private String mimeType;

        @Positive
        private long size;

        @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{64}$")
        private String sha256;

        @NotBlank @Size(max = 10)
        private String originalLang;

        @NotBlank @Size(max = 512)
        private String storageKey;
    }
}

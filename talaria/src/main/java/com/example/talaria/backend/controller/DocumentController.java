package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.document.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> create(
        @PathVariable("userId") long userId,
        @RequestPart("file") MultipartFile file,
        @RequestParam("original_name") String originalName,
        @RequestParam(value = "meeting_id", required = false) Long meetingId
) {
    try {
        // SHA-256 해시 계산
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(file.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String sha256 = sb.toString();

        // 파일 저장 로직
        var created = service.create(
                userId,
                meetingId,
                originalName,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                sha256,
                "ko", // originalLang 고정 예시
                "storageKeyExample" // storageKey 예시
        );

        return ResponseEntity.ok(created);
    } catch (IllegalStateException ex) {
        var pd = ProblemDetail.forStatus(409);
        pd.setTitle("Conflict");
        pd.setDetail("Duplicate filepath or constraint violation");
        pd.setInstance(URI.create("/api/users/" + userId + "/documents"));
        return ResponseEntity.status(409).body(Map.of(
                "type", "about:blank",
                "title", pd.getTitle(),
                "status", 409,
                "detail", pd.getDetail(),
                "instance", pd.getInstance().toString()
        ));
    } catch (IOException | NoSuchAlgorithmException e) {
        return ResponseEntity.status(500).body(Map.of("error", "파일 처리 중 오류 발생"));
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

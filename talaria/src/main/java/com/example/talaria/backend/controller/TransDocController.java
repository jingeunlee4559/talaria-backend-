package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.transdoc.TransDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}")
@Slf4j
public class TransDocController {

    private final TransDocService service;

    // 저장 루트: 번역 루트 우선, 없으면 문서 루트 사용
    @org.springframework.beans.factory.annotation.Value("${storage.transdocs.root:}")
    private String transRoot; // 예: ./data/uploads/trans

    @org.springframework.beans.factory.annotation.Value("${storage.documents.root:./data/uploads/docs}")
    private String docRoot;   // 폴백

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

    // ===== 신규: 다운로드 (requestId 기준) =====
    // GET /api/users/{userId}/trans-docs/req/{requestId}/download
    @GetMapping("/trans-docs/req/{requestId}/download")
    public ResponseEntity<?> downloadByRequestId(
            @PathVariable("userId") long userId,
            @PathVariable("requestId") String requestId
    ) {
        var info = service.getTransDownloadInfoByRequestId(userId, requestId);
        return streamTranslated(info);
    }

    // ===== 신규: 다운로드 ((docId, targetLang[, provider]) 기준 최신 1건) =====
    // GET /api/users/{userId}/documents/{docId}/trans-docs/lang/{targetLang}/download?provider=papago
    @GetMapping("/documents/{docId}/trans-docs/lang/{targetLang}/download")
    public ResponseEntity<?> downloadByComposite(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId,
            @PathVariable("targetLang") String targetLang,
            @RequestParam(name = "provider", required = false) String provider
    ) {
        var info = service.getTransDownloadInfoByComposite(userId, docId, targetLang, provider);
        return streamTranslated(info);
    }

    // ===== 내부 공통: 파일 스트리밍 =====
    private ResponseEntity<?> streamTranslated(Map<String, Object> info) {
        String rel = (String) info.get("translated_rel_path");
        if (rel == null || rel.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "type", "about:blank",
                    "title", "Not Found",
                    "status", 404,
                    "detail", "translated_rel_path not set"
            ));
        }

        String originalName = (String) info.getOrDefault("original_name", "translated");
        String targetLang   = (String) info.getOrDefault("target_language", "");
        String provider     = (String) info.getOrDefault("provider", "");

        // 파일 루트 결정: transRoot 우선, 없으면 docRoot 사용
        Path base = (transRoot != null && !transRoot.isBlank())
                ? Paths.get(transRoot).toAbsolutePath().normalize()
                : Paths.get(docRoot).toAbsolutePath().normalize();

        Path file = base.resolve(rel).normalize();

        // 보안 체크
        if (!file.startsWith(base)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "type", "about:blank",
                    "title", "Forbidden",
                    "status", 403,
                    "detail", "Invalid path resolved"
            ));
        }
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "type", "about:blank",
                    "title", "Not Found",
                    "status", 404,
                    "detail", "File not found on storage"
            ));
        }

        // 파일명 생성: {원본명(확장자 제거)}_{lang}[_{provider}].{변환확장자}
        String downloadName = buildDownloadName(originalName, file.getFileName().toString(), targetLang, provider);

        String mime = "application/octet-stream";
        try {
            String probed = Files.probeContentType(file);
            if (probed != null) mime = probed;
        } catch (Exception ignore) {}

        try {
            long size = Files.size(file);
            var res = new InputStreamResource(Files.newInputStream(file));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mime));
            headers.setContentLength(size);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(downloadName, StandardCharsets.UTF_8).build());

            return new ResponseEntity<>(res, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("Trans download failed: {}", rel, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "type", "about:blank",
                    "title", "Internal Server Error",
                    "status", 500,
                    "detail", "Failed to stream the translated file"
            ));
        }
    }

    private static String buildDownloadName(String originalName, String translatedFileName,
                                            String targetLang, String provider) {
        String base = stripExt(originalName);
        String ext  = extOf(translatedFileName);
        String suffix = (targetLang == null || targetLang.isBlank()) ? "" : "_" + targetLang;
        if (provider != null && !provider.isBlank()) suffix += "_" + provider;
        String name = base + suffix + (ext.isBlank() ? "" : "." + ext);
        return name.replaceAll("[\\\\/:*?\"<>|]", "_"); // sanitize
    }

    private static String stripExt(String name) {
        if (name == null) return "translated";
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }

    private static String extOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
        }
}

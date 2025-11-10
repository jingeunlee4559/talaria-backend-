package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.document.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/documents")
@Slf4j
public class DocumentController {

    private final DocumentService service;

    @org.springframework.beans.factory.annotation.Value("${storage.documents.root:./data/uploads/docs}")
    private String docRoot;

    // ===== 조회 =====
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

    // ===== 🎯 파일 내용 읽기 엔드포인트 (새로 추가) =====
    @GetMapping("/{docId}/content")
    public ResponseEntity<?> getDocumentContent(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId) {
        
        log.info("Get document content: userId={}, docId={}", userId, docId);
        
        try {
            var content = service.getDocumentContent(userId, docId);
            return ResponseEntity.ok(content);
            
        } catch (SecurityException se) {
            log.error("Security exception: userId={}, docId={}", userId, docId, se);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "접근 권한이 없습니다."));
        } catch (UnsupportedOperationException ue) {
            log.warn("Unsupported file type: userId={}, docId={}", userId, docId, ue);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ue.getMessage()));
        } catch (Exception e) {
            log.error("Get content failed: userId={}, docId={}", userId, docId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "파일 내용을 읽는 중 오류가 발생했습니다."));
        }
    }

    // ===== 다운로드 엔드포인트 =====
    @GetMapping("/{docId}/download")
    public ResponseEntity<?> download(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId) {
        
        log.info("Download requested: userId={}, docId={}", userId, docId);
        
        try {
            var f = service.getFileForDownload(userId, docId);

            Path file = f.path();
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                log.warn("File not found: {}", file);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "파일을 찾을 수 없습니다."));
            }

            String contentType = Files.probeContentType(file);
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            String downloadName = f.downloadName();
            String cdFilenameStar = "filename*=UTF-8''" + 
                java.net.URLEncoder.encode(downloadName, java.nio.charset.StandardCharsets.UTF_8);
            String cd = "attachment; " + cdFilenameStar;

            InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
            long size = Files.size(file);

            log.info("Download success: userId={}, docId={}, file={}", userId, docId, downloadName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(size))
                    .body(resource);

        } catch (SecurityException se) {
            log.error("Security exception: userId={}, docId={}", userId, docId, se);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "접근 권한이 없습니다."));
        } catch (Exception e) {
            log.error("Download failed: userId={}, docId={}", userId, docId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "다운로드 처리 중 오류가 발생했습니다."));
        }
    }

    // ===== 생성(업로드) =====
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @PathVariable("userId") long userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "original_name", required = false) String originalName,
            @RequestParam(name = "meeting_id",  required = false) Long meetingId,
            @RequestParam(name = "original_lang", defaultValue = "ko") String originalLang,
            @RequestParam(name = "use_context_rag", required = false) Boolean useContextRag
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "file is empty. multipart/form-data로 전송하고 필드명은 file이어야 합니다."
            ));
        }

        try {
            var info = writeFileAndHash(file);

            String effectiveOriginalName = StringUtils.hasText(originalName)
                    ? originalName
                    : info.originalName;

            var created = service.create(
                    userId,
                    meetingId,
                    effectiveOriginalName,
                    info.pathRelative,
                    info.mimeType,
                    info.size,
                    info.sha256,
                    originalLang,
                    info.pathAbsolute,
                    useContextRag != null && useContextRag
            );

            Object idVal = created.getOrDefault("doc_id", created.getOrDefault("id", null));
            String idStr = (idVal == null) ? "" : String.valueOf(idVal);
            var location = URI.create(String.format("/api/users/%d/documents/%s", userId, idStr));
            return ResponseEntity.created(location).body(created);

        } catch (DataIntegrityViolationException ex) {
            log.warn("Document create conflict", ex);
            return ResponseEntity.status(409).body(Map.of(
                    "type", "about:blank",
                    "title", "Conflict",
                    "status", 409,
                    "detail", "Duplicate key (e.g., filepath/sha256). 동일 파일 중복 업로드 가능성.",
                    "instance", String.format("/api/users/%d/documents", userId)
            ));
        } catch (MaxUploadSizeExceededException ex) {
            return ResponseEntity.status(413).body(Map.of(
                    "type", "about:blank",
                    "title", "Payload Too Large",
                    "status", 413,
                    "detail", "업로드 용량 제한을 초과했습니다.",
                    "instance", String.format("/api/users/%d/documents", userId)
            ));
        } catch (IOException | NoSuchAlgorithmException ex) {
            log.error("File write failed", ex);
            return ResponseEntity.status(500).body(Map.of(
                    "type", "about:blank",
                    "title", "Internal Server Error",
                    "status", 500,
                    "detail", "파일 저장 중 오류가 발생했습니다.",
                    "instance", String.format("/api/users/%d/documents", userId)
            ));
        }
    }

    // ===== 삭제 =====
    @DeleteMapping("/{docId}")
    public ResponseEntity<Map<String,Object>> delete(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId) {
        int n = service.delete(userId, docId);
        return ResponseEntity.ok(Map.of("deleted", n));
    }

    // ===== use_context_rag 토글 =====
    @PatchMapping("/{docId}/use-context-rag")
    public ResponseEntity<Map<String, Object>> toggleUseContextRag(
            @PathVariable("userId") long userId,
            @PathVariable("docId") long docId,
            @RequestBody(required = false) Map<String, Object> body) {
        
        log.info("Toggle useContextRag: userId={}, docId={}, body={}", userId, docId, body);
        
        var row = service.toggleUseContextRag(userId, docId);
        return ResponseEntity.ok(row);
    }

    // ===== 내부 유틸 =====
    private static String sanitizeFilename(String name) {
        if (name == null) return "upload.bin";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private record SavedInfo(
            String pathRelative,
            String pathAbsolute,
            String originalName,
            String mimeType,
            long size,
            String sha256
    ) {}

    private SavedInfo writeFileAndHash(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.bin");
        String safeName = sanitizeFilename(original);

        Path root = Paths.get(docRoot).normalize().toAbsolutePath();
        LocalDate today = LocalDate.now();
        Path dir = root.resolve(today.getYear()
                + "/" + String.format("%02d", today.getMonthValue())
                + "/" + String.format("%02d", today.getDayOfMonth()));
        Files.createDirectories(dir);

        String uniqueName = System.currentTimeMillis() + "_" + safeName;
        Path dest = dir.resolve(uniqueName);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(file.getInputStream());
             DigestInputStream din = new DigestInputStream(in, md)) {
            Files.copy(din, dest, StandardCopyOption.REPLACE_EXISTING);
        }

        String sha256 = toHex(md.digest());
        String probed = Files.probeContentType(dest);
        String mimeType = (file.getContentType() != null && !file.getContentType().isBlank())
                ? file.getContentType()
                : (probed != null ? probed : "application/octet-stream");
        long size = Files.size(dest);

        String abs = dest.toAbsolutePath().toString();
        String rel = root.relativize(dest).toString().replace('\\', '/');

        log.info("DOC_WRITE ok abs={} rel={} size={} type={} sha256={}", abs, rel, size, mimeType, sha256);
        return new SavedInfo(rel, abs, safeName, mimeType, size, sha256);
    }

    @Data
    public static class CreateDocumentReq {
        private Long meetingId;
        @NotBlank @Size(max = 255) private String originalName;
        @NotBlank @Size(max = 255) private String filepath;
        @NotBlank @Size(max = 100) private String mimeType;
        @Positive private long size;
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{64}$") private String sha256;
        @NotBlank @Size(max = 10) private String originalLang;
        @NotBlank @Size(max = 512) private String storageKey;
    }

    @Data
    public static class ToggleUseContextRagRequest {
        @NotNull
        private Boolean enabled;
    }
}
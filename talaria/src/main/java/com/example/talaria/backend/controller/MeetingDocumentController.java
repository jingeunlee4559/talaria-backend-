package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.document.MeetingDocumentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings/{meetingId}/documents")
@Slf4j
public class MeetingDocumentController {

    private final MeetingDocumentService service;

    // 문서 저장 루트 (DocumentController와 동일 키)
    @org.springframework.beans.factory.annotation.Value("${storage.documents.root:./data/uploads/docs}")
    private String docRoot;

    // ===== 기존: 전체 목록(권한 필터 없음, 호환 유지) =====
    @GetMapping
    public ResponseEntity<?> list(@PathVariable("userId") long userId,
                                  @PathVariable("meetingId") long meetingId) {
        return ResponseEntity.ok(Map.of("items", service.listByMeeting(meetingId)));
    }

    // ===== 신규: 권한 포함 페이지 목록 =====
    // GET /api/users/{userId}/meetings/{meetingId}/documents/page?page=0&size=20
    @GetMapping("/page")
    public ResponseEntity<PageResponse> listPaged(@PathVariable("userId") long userId,
                                                  @PathVariable("meetingId") long meetingId,
                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listByMeetingPaged(userId, meetingId, page, size));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> attach(@PathVariable("userId") long userId,
                                    @PathVariable("meetingId") long meetingId,
                                    @RequestBody AttachReq req) {
        try {
            if (req == null || req.getDocIds() == null || req.getDocIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "docIds 가 비어 있습니다."));
            }
            var result = service.attachDocs(userId, meetingId, req.getDocIds());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ise.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", iae.getMessage()));
        } catch (Exception e) {
            log.error("Attach docs error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "문서 첨부 처리 중 오류가 발생했습니다."));
        }
    }

    // 단건 해제
    @DeleteMapping("/{docId}")
    public ResponseEntity<?> detach(@PathVariable("userId") long userId,
                                    @PathVariable("meetingId") long meetingId,
                                    @PathVariable("docId") long docId) {
        return ResponseEntity.ok(service.detachDoc(meetingId, docId));
    }

    // 배치 해제 (선택)
    @PostMapping(path = "/detach", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> detachBatch(@PathVariable("userId") long userId,
                                         @PathVariable("meetingId") long meetingId,
                                         @RequestBody DetachReq req) {
        if (req == null || req.getDocIds() == null || req.getDocIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "docIds 가 비어 있습니다."));
        }
        var result = service.detachDocs(meetingId, req.getDocIds());
        return ResponseEntity.ok(result);
    }

    // ===== 신규: 다운로드 =====
    // GET /api/users/{userId}/meetings/{meetingId}/documents/{docId}/download
    @GetMapping("/{docId}/download")
    public ResponseEntity<?> download(@PathVariable("userId") long userId,
                                      @PathVariable("meetingId") long meetingId,
                                      @PathVariable("docId") long docId) {
        var info = service.getDownloadInfo(userId, meetingId, docId);

        String rel = (String) info.get("filepath");
        String originalName = (String) info.getOrDefault("original_name", "download.bin");
        String mime = (String) info.getOrDefault("mime_type", "application/octet-stream");

        try {
            Path root = Paths.get(docRoot).toAbsolutePath().normalize();
            Path file = root.resolve(rel).normalize();

            if (!file.startsWith(root)) {
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

            try {
                String probed = Files.probeContentType(file);
                if (probed != null) mime = probed;
            } catch (Exception ignore) {}

            long size = Files.size(file);
            var res = new InputStreamResource(Files.newInputStream(file));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mime));
            headers.setContentLength(size);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(originalName, StandardCharsets.UTF_8).build());

            return new ResponseEntity<>(res, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("Download failed: {}", rel, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "type", "about:blank",
                    "title", "Internal Server Error",
                    "status", 500,
                    "detail", "Failed to stream the file"
            ));
        }
    }

    @Data
    public static class AttachReq {
        private List<Long> docIds;
    }

    @Data
    public static class DetachReq {
        private List<Long> docIds;
    }
}

// src/main/java/com/example/talaria/backend/controller/MeetingDocumentController.java
package com.example.talaria.backend.controller;

import com.example.talaria.backend.service.document.MeetingDocumentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings/{meetingId}/documents")
@Slf4j
public class MeetingDocumentController {

    private final MeetingDocumentService service;

    @Value("${storage.documents.root:./data/uploads/docs}")
    private String docRoot;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable long userId, @PathVariable long meetingId) {
        try {
            List<Map<String, Object>> items = service.listByMeeting(meetingId);
            return ResponseEntity.ok(Map.of("items", items));
        } catch (Exception e) {
            log.error("문서 목록 조회 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "문서 목록 조회 중 오류가 발생했습니다."));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> attach(@PathVariable long userId, @PathVariable long meetingId,
                                    @RequestBody AttachReq request) {
        try {
            if (request == null ||
                ((request.getDocIds() == null || request.getDocIds().isEmpty()) &&
                 (request.getTransDocIds() == null || request.getTransDocIds().isEmpty()))) {
                return ResponseEntity.badRequest().body(Map.of("error", "docIds 또는 transDocIds 중 하나는 필수입니다."));
            }
            var result = service.attachDocuments(userId, meetingId, request);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ise.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", iae.getMessage()));
        } catch (Exception e) {
            log.error("문서 추가 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "문서 추가 중 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<?> detach(@PathVariable long userId, @PathVariable long meetingId, @PathVariable long docId) {
        try {
            service.detachDocument(meetingId, docId);
            return ResponseEntity.ok(Map.of("message", "문서가 해제되었습니다."));
        } catch (Exception e) {
            log.error("문서 해제 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "문서 해제 중 오류가 발생했습니다."));
        }
    }

    // 원문 다운로드
    @GetMapping("/{docId}/download")
    public ResponseEntity<?> download(@PathVariable long userId, @PathVariable long meetingId, @PathVariable long docId) {
        try {
            var info = service.getOriginalDownloadInfo(userId, meetingId, docId);
            String relPath = (String) info.get("filepath");
            String originalName = (String) info.getOrDefault("original_name", "download.bin");
            String mimeType = (String) info.getOrDefault("mime_type", "application/octet-stream");

            Path root = Paths.get(docRoot).toAbsolutePath().normalize();
            Path file = root.resolve(relPath).normalize();

            if (!file.startsWith(root)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "잘못된 파일 경로입니다."));
            if (!Files.exists(file) || !Files.isRegularFile(file)) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "파일을 찾을 수 없습니다."));

            long size = Files.size(file);
            var resource = new InputStreamResource(Files.newInputStream(file));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setContentLength(size);
            headers.setContentDisposition(ContentDisposition.attachment().filename(originalName, StandardCharsets.UTF_8).build());

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("파일 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "파일 다운로드 중 오류가 발생했습니다."));
        }
    }

    //  번역 다운로드 (프론트에서 번역행에 대해 이 엔드포인트 호출)
    @GetMapping("/translations/{transDocId}/download")
    public ResponseEntity<?> downloadTranslated(@PathVariable long userId, @PathVariable long meetingId, @PathVariable long transDocId) {
        try {
            var info = service.getTranslationDownloadInfo(userId, meetingId, transDocId);
            String relPath = (String) info.get("filepath");
            String originalName = (String) info.getOrDefault("original_name", "translated.bin");
            String mimeType = (String) info.getOrDefault("mime_type", "application/octet-stream");

            Path root = Paths.get(docRoot).toAbsolutePath().normalize();
            Path file = root.resolve(relPath).normalize();

            if (!file.startsWith(root)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "잘못된 파일 경로입니다."));
            if (!Files.exists(file) || !Files.isRegularFile(file)) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "파일을 찾을 수 없습니다."));

            long size = Files.size(file);
            var resource = new InputStreamResource(Files.newInputStream(file));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setContentLength(size);
            headers.setContentDisposition(ContentDisposition.attachment().filename(originalName, StandardCharsets.UTF_8).build());

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("번역 파일 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "파일 다운로드 중 오류가 발생했습니다."));
        }
    }

    @Data
    public static class AttachReq {
        private List<Long> docIds;
        private List<Long> transDocIds;
    }
}

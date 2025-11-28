package com.example.talaria.backend.controller;

import com.example.talaria.backend.service.document.DocumentService;
import com.example.talaria.backend.service.document.TransDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings/{meetingId}/documents")
@Slf4j
public class MeetingDocumentPreviewController {

    private final DocumentService documentService;
    private final TransDocumentService transDocumentService;

    // ====================================================
    // 1) 회의방 전용 "원본 PDF 미리보기"
    //    GET /api/users/{userId}/meetings/{meetingId}/documents/{docId}/preview
    //    - docId : tl_documents.doc_id
    //    - 내부에서 tl_meeting_documents로 회의 소속 검증
    // ====================================================
    @GetMapping("/{docId}/preview")
    public ResponseEntity<?> previewOriginalInMeeting(
            @PathVariable("userId") long userId,
            @PathVariable("meetingId") long meetingId,
            @PathVariable("docId") long docId
    ) {
        log.info("Preview ORIGINAL requested: userId={}, meetingId={}, docId={}",
                userId, meetingId, docId);

        try {
            // 회의 기준( tl_meeting_documents )으로 권한 검증 + 파일 메타 조회
            DocumentService.DownloadFile f =
                    documentService.getFileForMeetingPreview(userId, meetingId, docId);

            Path file = f.path();
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                log.warn("Original file not found: {}", file);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "파일을 찾을 수 없습니다."));
            }

            String contentType = f.mimeType();
            if (contentType == null || contentType.isBlank()) {
                contentType = Files.probeContentType(file);
            }
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            // 🔒 PDF만 미리보기 허용
            if (!"application/pdf".equalsIgnoreCase(contentType)) {
                log.warn("Unsupported preview type for original: type={}, file={}", contentType, file);
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(Map.of("error", "원본 PDF 파일만 미리보기를 지원합니다."));
            }

            String previewName = f.downloadName();
            String cdFilenameStar =
                    "filename*=UTF-8''" +
                            java.net.URLEncoder.encode(previewName, java.nio.charset.StandardCharsets.UTF_8);
            String cd = "inline; " + cdFilenameStar;

            InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
            long size = Files.size(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd) // ★ inline (브라우저에서 바로 보기)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(size))
                    .body(resource);

        } catch (SecurityException se) {
            log.error("Security exception (original preview): userId={}, meetingId={}, docId={}",
                    userId, meetingId, docId, se);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "접근 권한이 없습니다."));
        } catch (Exception e) {
            log.error("Original preview failed: userId={}, meetingId={}, docId={}",
            userId, meetingId, docId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "미리보기 처리 중 오류가 발생했습니다.",
                            "detail", e.getClass().getName() + ": " + e.getMessage()
                    ));
        }
    }

    // ====================================================
    // 2) 회의방 전용 "번역 PDF 미리보기"
    //    GET /api/users/{userId}/meetings/{meetingId}/documents/translations/{transDocId}/preview
    //    - transDocId : tl_trans_docs.trans_doc_id
    //    - 내부에서 tl_meeting_documents로 회의 소속 검증
    // ====================================================
    @GetMapping("/translations/{transDocId}/preview")
    public ResponseEntity<?> previewTranslatedInMeeting(
            @PathVariable("userId") long userId,
            @PathVariable("meetingId") long meetingId,
            @PathVariable("transDocId") long transDocId
    ) {
        log.info("Preview TRANSLATED requested: userId={}, meetingId={}, transDocId={}",
                userId, meetingId, transDocId);

        try {
            // 회의 기준( tl_meeting_documents )으로 권한 검증 + 번역 파일 메타 조회
            TransDocumentService.DownloadFile f =
                    transDocumentService.getFileForMeetingPreview(userId, meetingId, transDocId);

            Path file = f.path();
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                log.warn("Translated file not found: {}", file);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "번역 파일을 찾을 수 없습니다."));
            }

            String contentType = f.mimeType();
            if (contentType == null || contentType.isBlank()) {
                contentType = Files.probeContentType(file);
            }
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            //  PDF 번역본만 미리보기 허용
            if (!"application/pdf".equalsIgnoreCase(contentType)) {
                log.warn("Unsupported preview type for translated: type={}, file={}", contentType, file);
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(Map.of("error", "PDF 번역본만 미리보기를 지원합니다."));
            }

            String previewName = f.downloadName();
            String cdFilenameStar =
                    "filename*=UTF-8''" +
                            java.net.URLEncoder.encode(previewName, java.nio.charset.StandardCharsets.UTF_8);
            String cd = "inline; " + cdFilenameStar;

            InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
            long size = Files.size(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(size))
                    .body(resource);

        } catch (SecurityException se) {
            log.error("Security exception (translated preview): userId={}, meetingId={}, transDocId={}",
                    userId, meetingId, transDocId, se);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "접근 권한이 없습니다."));
        } catch (Exception e) {
            log.error("Translated preview failed: userId={}, meetingId={}, transDocId={}",
                    userId, meetingId, transDocId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "번역 미리보기 처리 중 오류가 발생했습니다.",
                            "detail", e.getClass().getName() + ": " + e.getMessage()
                    ));
        }
    }
}

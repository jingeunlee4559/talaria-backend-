package com.example.talaria.backend.controller;

import com.example.talaria.backend.service.document.MeetingDocumentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings/{meetingId}/documents")
@Slf4j
public class MeetingDocumentController {

    private final MeetingDocumentService service;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable("userId") long userId,
                                  @PathVariable("meetingId") long meetingId) {
        return ResponseEntity.ok(Map.of("items", service.listByMeeting(meetingId)));
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

    @Data
    public static class AttachReq {
        private List<Long> docIds;
    }

    @Data
    public static class DetachReq {
        private List<Long> docIds;
    }
}

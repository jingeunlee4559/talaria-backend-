package com.example.talaria.backend.controller;

import com.example.talaria.backend.service.meeting.MeetingTagService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
public class MeetingTagController {

    private final MeetingTagService service;

    /** 마스터 태그 목록(선택지) */
    @GetMapping("/tags")
    public ResponseEntity<List<Map<String,Object>>> listMasterTags(
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(service.listMasterTags(activeOnly));
    }

    /** 특정 회의에 현재 부착된 태그 목록 */
    @GetMapping("/users/{userId}/meetings/{meetingId}/tags")
    public ResponseEntity<List<Map<String,Object>>> listAssigned(
            @PathVariable long userId,
            @PathVariable long meetingId) {
        return ResponseEntity.ok(service.listAssigned(meetingId));
    }

    /** 단건 첨부(토글 ON) */
    @PostMapping("/users/{userId}/meetings/{meetingId}/tags/{tagId}")
    public ResponseEntity<Map<String,Object>> attach(
            @PathVariable long userId,
            @PathVariable long meetingId,
            @PathVariable long tagId) {
        try {
            return ResponseEntity.ok(service.attach(userId, meetingId, tagId));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","internal error"));
        }
    }

    /** 단건 해제(토글 OFF) */
    @DeleteMapping("/users/{userId}/meetings/{meetingId}/tags/{tagId}")
    public ResponseEntity<Map<String,Object>> detach(
            @PathVariable long userId,
            @PathVariable long meetingId,
            @PathVariable long tagId) {
        try {
            return ResponseEntity.ok(service.detach(meetingId, tagId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","internal error"));
        }
    }

    /** 선택형 UI에 맞춘 벌크 교체 */
    @PutMapping("/users/{userId}/meetings/{meetingId}/tags")
    public ResponseEntity<Map<String,Object>> replaceAll(
            @PathVariable long userId,
            @PathVariable long meetingId,
            @Valid @RequestBody ReplaceReq req) {
        try {
            return ResponseEntity.ok(service.replaceAll(userId, meetingId, req.getTagIds()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","internal error"));
        }
    }

    @Data
    public static class ReplaceReq {
        @NotNull
        private List<Long> tagIds; // 빈 배열 허용 → 전체 제거
    }
}

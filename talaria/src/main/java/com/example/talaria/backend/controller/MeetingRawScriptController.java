package com.example.talaria.backend.controller;

import com.example.talaria.backend.service.meeting.MeetingRawScriptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings/{meetingId}/raw-script")
public class MeetingRawScriptController {

    private final MeetingRawScriptService rawScriptService;

    /**
     * 프론트 → 백엔드
     *
     * POST /api/users/{userId}/meetings/{meetingId}/raw-script
     *
     * Body:
     * {
     *   "meetingTitle": "11월 정기 개발 회의",
     *   "participants": ["김철수", "이영희"]
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> buildAndSend(
            @PathVariable("userId") long userId,
            @PathVariable("meetingId") long meetingId,
            @RequestBody @Valid RawScriptRequest req
    ) {

        log.info("[RawScript] request: userId={}, meetingId={}, title='{}', participants={}",
                userId, meetingId, req.getMeetingTitle(), req.getParticipants());

        Map<String, Object> res = rawScriptService.buildAndSendRawScript(
                meetingId,
                req.getMeetingTitle(),
                req.getParticipants()
        );

        return ResponseEntity.ok(res);
    }

    // ====== 요청 DTO ======

    @Data
    public static class RawScriptRequest {

        @NotBlank(message = "meetingTitle은 필수입니다.")
        private String meetingTitle;

        @NotEmpty(message = "participants는 1명 이상이어야 합니다.")
        private List<@NotBlank String> participants;
    }
}

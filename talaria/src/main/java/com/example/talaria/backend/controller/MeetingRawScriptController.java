package com.example.talaria.backend.controller;

import com.example.talaria.backend.service.meeting.MeetingRawScriptService;
import com.example.talaria.backend.service.meeting.MeetingService;

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
    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> buildAndSend(
            @PathVariable("userId") long userId,
            @PathVariable("meetingId") long meetingId,
            @RequestBody @Valid RawScriptRequest req
    ) {

        log.info("[RawScript] request: userId={}, meetingId={}, title='{}', participants={}",
                userId, meetingId, req.getMeetingTitle(), req.getParticipants());

        // 1) tl_meetings 에서 회의 정보 조회 (scheduled_at 포함)
        Map<String, Object> meeting = meetingService.getById(meetingId);
        if (meeting == null) {
            log.warn("[RawScript] meeting not found: meetingId={}", meetingId);
            return ResponseEntity.notFound().build();
        }
        Object scheduledAt = meeting.get("scheduled_at");

        // 2) raw-script 생성 + 파이썬 호출
        Map<String, Object> res = rawScriptService.buildAndSendRawScript(
                meetingId,
                req.getMeetingTitle(),
                req.getParticipants()
        );

        // 3) 응답에 scheduledAt 추가
        res.put("scheduledAt", scheduledAt);

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

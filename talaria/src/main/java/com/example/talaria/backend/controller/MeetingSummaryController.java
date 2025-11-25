package com.example.talaria.backend.controller;

import com.example.talaria.backend.service.meeting.MeetingSummaryQueryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings/{meetingId}")
public class MeetingSummaryController {

    private final MeetingSummaryQueryService meetingSummaryQueryService;

    /**
     * 회의 요약 + 발화 스크립트 조회
     *
     * GET /api/users/{userId}/meetings/{meetingId}/summary?lang=ko
     *
     * Response 예시:
     * {
     *   "meetingId": 65,
     *   "lang": "ko",
     *   "summary": {
     *     "summary_id": ...,
     *     "meeting_id": ...,
     *     "lang": "ko",
     *     "meeting_date": "...",
     *     "meeting_title": "...",
     *     "participants": "...",
     *     "participant_count": 3,
     *     "summary": "...",
     *     "meeting_score": 80
     *   },
     *   "utterances": [
     *     { "speaker_id": "user-xxx", "text": "..." },
     *     { "speaker_id": "user-yyy", "text": "..." }
     *   ]
     * }
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @PathVariable("userId") long userId,
            @PathVariable("meetingId") long meetingId,
            @RequestParam(name = "lang") @NotBlank String lang
    ) {
        log.info("[MeetingSummary] userId={}, meetingId={}, lang={}", userId, meetingId, lang);

        Map<String, Object> body =
                meetingSummaryQueryService.getSummaryWithUtterances(meetingId, lang);

        return ResponseEntity.ok(body);
    }
}

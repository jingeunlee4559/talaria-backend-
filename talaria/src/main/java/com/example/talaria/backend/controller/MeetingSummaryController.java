package com.example.talaria.backend.controller;

import com.example.talaria.backend.service.meeting.MeetingSummaryQueryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/meetings")
public class MeetingSummaryController {

    private final MeetingSummaryQueryService meetingSummaryQueryService;

    /**
     * 회의 요약 + 발화 스크립트 조회 (단일 회의)
     *
     * GET /api/users/{userId}/meetings/{meetingId}/summary?lang=ko
     */
    @GetMapping("/{meetingId}/summary")
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

    /**
     * 유저의 전체 회의 요약 리스트 조회
     *
     * GET /api/users/{userId}/meetings/summary/all?lang=ko
     */
    @GetMapping("/summary/all")
    public ResponseEntity<List<Map<String, Object>>> getAllSummaries(
            @PathVariable("userId") long userId,
            @RequestParam("lang") String lang
    ) {
        log.info("[MeetingSummary-All] userId={}, lang={}", userId, lang);

        List<Map<String, Object>> summaries =
                meetingSummaryQueryService.getAllSummaries(userId, lang);

        return ResponseEntity.ok(summaries);
    }
}

package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.manner.MannerEventService;
import com.example.talaria.backend.service.manner.MannerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Manner", description = "매너체커 룰 및 쿨다운 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manner")
public class MannerController {

    private final MannerQueryService queryService;
    private final MannerEventService eventService;

    // --------------------------
    // 1) 룰 조회
    // --------------------------
    @Operation(summary = "룰 조회 (페이징)",
            description = "로케일 prefix 매칭: speakerLocale='ko-KR'이면 rule.speaker_locale='ko'를 매칭으로 간주")
    @GetMapping("/rules")
    public ResponseEntity<PageResponse> listRules(
            @RequestParam(required = false) Integer active,
            @RequestParam(required = false) String agents,          // CSV: direct,negative,...
            @RequestParam(required = false) String speakerLocale,   // 예: ko-KR
            @RequestParam(required = false) String listenerLocale,  // 예: en-US
            @RequestParam(required = false) String culturePair,     // 예: ko->en
            @RequestParam(required = false) String versionTag,      // 예: v1
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        List<String> agentCodes = null;
        if (agents != null && !agents.isBlank()) {
            agentCodes = Arrays.stream(agents.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        var resp = queryService.list(active, agentCodes, speakerLocale, listenerLocale, culturePair, versionTag, page, size);
        return ResponseEntity.ok(resp);
    }

    // --------------------------
    // 2) 쿨다운 체크 & 마킹 (원자적)
    // --------------------------
    @Operation(summary = "쿨다운 체크 & 마킹",
            description = "허용 시 last_alert_at를 NOW()로 삽입/갱신. 허용이면 allowed=true, 거부면 false.")
    @PostMapping("/events/check-and-mark")
    public ResponseEntity<CheckAndMarkResponse> checkAndMark(@RequestBody CheckAndMarkRequest req) {
        boolean allowed = eventService.checkAndMark(req.getRuleId(), req.getSessionId(), req.getSpeakerId(), req.getCooldownSec());
        var out = new CheckAndMarkResponse(allowed, Instant.now().toEpochMilli(), req.getCooldownSec());
        return ResponseEntity.ok(out);
    }

    // DTOs
    @Data
    public static class CheckAndMarkRequest {
        @NotNull
        private Long ruleId;
        private Long sessionId;   // nullable 허용
        private String speakerId; // nullable 허용
        @Min(0)
        private int cooldownSec;
    }

    @Data
    public static class CheckAndMarkResponse {
        private final boolean allowed;
        private final long serverEpochMs;
        private final int cooldownSec;
    }
}

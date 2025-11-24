package com.example.talaria.backend.service.meeting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRawScriptClient {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${talaria.summarizeUrl:}")
    private String summarizeUrl;

    /**
     * Python 서버로 회의 요약 요청 전송
     * - URL: {summarizeUrl} (예: http://localhost:8000/summary/process)
     * - Body 스키마:
     *   {
     *     "meeting_id": "MTG_2025_001",
     *     "current_date": "2025-11-24",
     *     "meeting_title": "1분기 제품 기획 회의",
     *     "participants": "김철수, 이영희, 박민수",
     *     "transcript": "김철수: ..."
     *   }
     */
    public Map<String, Object> sendRawScript(long meetingId, String rawScriptText) {
        if (summarizeUrl == null || summarizeUrl.isBlank()) {
            throw new IllegalStateException("talaria.summarizeUrl 설정이 비어 있습니다. (.env 또는 application.yaml 확인)");
        }

        // =========================
        // 1) rawScriptText 파싱
        //     [회의 정보]
        //     주제: ...
        //     참석자: ...
        //
        //     [스크립트]
        //     화자: 발화 ...
        // =========================
        String meetingTitle = "";
        String participants = "";
        String transcript = rawScriptText != null ? rawScriptText : "";

        if (rawScriptText != null && !rawScriptText.isBlank()) {
            String[] lines = rawScriptText.split("\\R"); // \n, \r\n 모두 대응
            int scriptStartIdx = -1;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                if (line.startsWith("주제:")) {
                    meetingTitle = line.substring("주제:".length()).trim();
                } else if (line.startsWith("참석자:")) {
                    participants = line.substring("참석자:".length()).trim();
                } else if (line.equals("[스크립트]")) {
                    scriptStartIdx = i + 1; // 다음 줄부터 실제 대화
                    break;
                }
            }

            // [스크립트] 이후 줄들만 transcript로 사용
            if (scriptStartIdx >= 0 && scriptStartIdx < lines.length) {
                StringBuilder sb = new StringBuilder();
                for (int i = scriptStartIdx; i < lines.length; i++) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(lines[i]);
                }
                transcript = sb.toString();
            }
        }

        // =========================
        // 2) Python MeetingSummaryRequest 바디 구성
        // =========================
        Map<String, Object> body = new HashMap<>();

        // meeting_id: 예시는 "MTG_2025_001" 이지만, 여기선 일단 단순히 ID 문자열 사용
        // 필요하면 포맷을 "MTG_" + meetingId 같은 식으로 변경 가능.
        body.put("meeting_id", String.valueOf(meetingId));

        // current_date: 오늘 날짜 (yyyy-MM-dd)
        body.put("current_date", LocalDate.now().toString());

        body.put("meeting_title", meetingTitle);
        body.put("participants", participants);  // "이름1, 이름2, ..."
        body.put("transcript", transcript);      // "화자: 발화..." 줄바꿈 포함

        log.info("[RawScript] summary request body for meetingId={}: {}", meetingId, body);

        // =========================
        // 3) HTTP POST 전송
        // =========================
        try {
            Mono<Map> mono = webClient.post()
                    .uri(summarizeUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class);

            Map<String, Object> res = mono.block();
            if (res == null) {
                log.warn("[RawScript] summary server returned null for meetingId={}", meetingId);
                res = new HashMap<>();
                res.put("status", "no_response");
            } else {
                log.info("[RawScript] summary response for meetingId={} -> {}", meetingId, res);
            }
            return res;

        } catch (WebClientResponseException e) {
            log.error("[RawScript] summary HTTP error for meetingId={} status={} body={}",
                    meetingId, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            log.error("[RawScript] summary call failed for meetingId={}", meetingId, e);
            throw e;
        }
    }
}

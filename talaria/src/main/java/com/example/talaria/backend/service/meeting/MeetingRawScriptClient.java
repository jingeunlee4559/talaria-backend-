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

    public Map<String, Object> sendRawScript(long meetingId, String rawScriptText) {
        if (summarizeUrl == null || summarizeUrl.isBlank()) {
            throw new IllegalStateException("talaria.summarizeUrl 설정이 비어 있습니다. (.env 또는 application.yaml 확인)");
        }

        // =========================
        // rawScriptText 파싱
        // =========================
        String meetingTitle = "";
        String participants = "";
        String transcript = rawScriptText != null ? rawScriptText : "";

        if (rawScriptText != null && !rawScriptText.isBlank()) {
            String[] lines = rawScriptText.split("\\R"); // 모든 줄바꿈(\n, \r\n) 대응
            int scriptStartIdx = -1;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                if (line.startsWith("주제:")) {
                    meetingTitle = line.substring("주제:".length()).trim();
                } else if (line.startsWith("참석자:")) {
                    participants = line.substring("참석자:".length()).trim();
                } else if (line.equals("[스크립트]")) {
                    // 바로 다음 줄부터 실제 발화 스크립트
                    scriptStartIdx = i + 1;
                    break;
                }
            }

            // [스크립트] 이후 부분만 transcript로 사용
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
        // 요청 바디 구성
        // =========================
        Map<String, Object> body = new HashMap<>();
        body.put("meeting_id", String.valueOf(meetingId));          // 문자열 ID
        body.put("current_date", LocalDate.now().toString());       // yyyy-MM-dd
        body.put("meeting_time", "");                               // 시간 정보가 없으니 일단 빈 값
        body.put("meeting_title", meetingTitle);                    // 파싱된 제목 (없으면 "")
        body.put("participants", participants);                     // "이름1, 이름2, ..."
        body.put("transcript", transcript);                         // 실제 대화 스크립트

        log.info("[RawScript] summarize request body for meetingId={}: {}", meetingId, body);

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
                log.warn("[RawScript] summarize returned null for meetingId={}", meetingId);
                res = new HashMap<>();
                res.put("status", "no_response");
            } else {
                log.info("[RawScript] summarize response for meetingId={} -> {}",
                        meetingId, res);
            }
            return res;

        } catch (WebClientResponseException e) {
            log.error("[RawScript] summarize HTTP error for meetingId={} status={} body={}",
                    meetingId, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            log.error("[RawScript] summarize call failed for meetingId={}", meetingId, e);
            throw e;
        }
    }
}

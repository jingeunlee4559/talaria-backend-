package com.example.talaria.backend.service.meeting;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * talaria-ai (파이썬) 쪽으로 raw script text를 보내는 클라이언트
 */
@Component
@RequiredArgsConstructor
public class MeetingRawScriptClient {

    /**
     * talaria-ai 용 WebClient
     * 예) Config 쪽에서
     *   @Bean
     *   @Qualifier("aiClient")
     *   WebClient aiClient() { ... }
     */
    @Qualifier("aiClient")
    private final WebClient aiClient;

    /**
     * raw script text를 파이썬 서비스에 전송
     *
     * 요청 JSON 예시:
     * {
     *   "meeting_id": 123,
     *   "raw_script": "[회의 정보] ... [스크립트] ..."
     * }
     */
    public Map<String, Object> sendRawScript(long meetingId, String rawScriptText) {
        Map<String, Object> body = new HashMap<>();
        body.put("meeting_id", meetingId);
        body.put("raw_script", rawScriptText);

        Mono<Map> mono = aiClient.post()
                .uri("/meeting/raw-script")   // 파이썬 실제 경로에 맞게 변경
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class);

        Map<String, Object> res = mono.block();
        if (res == null) {
            res = new HashMap<>();
            res.put("status", "no_response");
        }
        return res;
    }
}

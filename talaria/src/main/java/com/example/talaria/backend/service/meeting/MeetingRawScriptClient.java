package com.example.talaria.backend.service.meeting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRawScriptClient {

    @Qualifier("aiClient")
    private final WebClient aiClient;

    /**
     * raw script text를 파이썬 서비스에 전송
     */
    public Map<String, Object> sendRawScript(long meetingId, String rawScriptText) {
        Map<String, Object> body = new HashMap<>();
        body.put("meeting_id", meetingId);
        body.put("raw_script", rawScriptText);

        log.info("[RawScript] sending to AI: meetingId={}, length={}",
                meetingId, (rawScriptText != null ? rawScriptText.length() : 0));

        try {
            Mono<Map> mono = aiClient.post()
                    .uri("/meeting/raw-script")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class);

            Map<String, Object> res = mono.block();

            if (res == null) {
                log.warn("[RawScript] AI returned null response for meetingId={}", meetingId);
                res = new HashMap<>();
                res.put("status", "no_response");
            } else {
                log.info("[RawScript] AI response for meetingId={} -> {}", meetingId, res);
            }
            return res;
        } catch (WebClientResponseException e) {
            log.error("[RawScript] AI HTTP error for meetingId={} status={} body={}",
                    meetingId, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            log.error("[RawScript] AI call failed for meetingId={}", meetingId, e);
            throw e;
        }
    }
}

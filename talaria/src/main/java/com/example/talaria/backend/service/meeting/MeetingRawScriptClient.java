package com.example.talaria.backend.service.meeting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final WebClient webClient = WebClient.builder().build();

    @Value("${talaria.summarizeUrl:}")
    private String summarizeUrl;

    public Map<String, Object> sendRawScript(long meetingId, String rawScriptText) {
    if (summarizeUrl == null || summarizeUrl.isBlank()) {
        throw new IllegalStateException("talaria.summarizeUrl 설정이 비어 있습니다. (.env 또는 application.yaml 확인)");
    }

    // ★ 여기만 수정
    Map<String, Object> body = new HashMap<>();
    body.put("text", rawScriptText);  // 코랩이 요구하는 필드명에 맞춤

    log.info("[RawScript] calling Colab summarize: url={}, meetingId={}, length={}",
            summarizeUrl, meetingId, (rawScriptText != null ? rawScriptText.length() : 0));

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
                log.warn("[RawScript] Colab summarize returned null for meetingId={}", meetingId);
                res = new HashMap<>();
                res.put("status", "no_response");
            } else {
                log.info("[RawScript] Colab summarize response for meetingId={} -> {}",
                        meetingId, res);
            }
            return res;

        } catch (WebClientResponseException e) {
            // ★ 여기에서 FastAPI 422의 body를 그대로 찍어줌
            log.error("[RawScript] Colab HTTP error for meetingId={} status={} body={}",
                    meetingId, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            throw e; // 일단 그대로 다시 던짐 (지금처럼 500으로 올라감)
        } catch (Exception e) {
            log.error("[RawScript] Colab call failed for meetingId={}", meetingId, e);
            throw e;
        }
    }
}

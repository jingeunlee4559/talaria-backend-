package com.example.talaria.backend.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiProxyController {

    private final WebClient aiClient;

    public AiProxyController(@Qualifier("aiClient") WebClient aiClient) {
        this.aiClient = aiClient;
    }

    @PostMapping("/stt")
    public Mono<String> stt(@RequestBody Map<String, Object> req) {
        return aiClient.post().uri("/stt").bodyValue(req).retrieve().bodyToMono(String.class);
    }

    @PostMapping("/translate")
    public Mono<String> translate(@RequestBody Map<String, Object> req) {
        return aiClient.post().uri("/translate").bodyValue(req).retrieve().bodyToMono(String.class);
    }

    @PostMapping("/tts")
    public Mono<String> tts(@RequestBody Map<String, Object> req) {
        return aiClient.post().uri("/tts").bodyValue(req).retrieve().bodyToMono(String.class);
    }

    // 문화 팁(매너 가이드)
    @PostMapping("/advice")
    public Mono<String> advice(@RequestBody Map<String, Object> req) {
        return aiClient.post().uri("/advice").bodyValue(req).retrieve().bodyToMono(String.class);
    }
}

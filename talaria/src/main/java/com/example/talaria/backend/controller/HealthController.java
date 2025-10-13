package com.example.talaria.backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/healthz")
    public Map<String, Object> health() {
        return Map.of("ok", true, "service", "talaria-backend");
    }
}

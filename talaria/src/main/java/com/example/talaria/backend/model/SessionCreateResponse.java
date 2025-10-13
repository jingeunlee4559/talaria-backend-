package com.example.talaria.backend.model;

public record SessionCreateResponse(
        String session_id,
        String ws_url,
        String token,
        long expires_at
) {}
package com.example.talaria.backend.model;

public record SessionSettingsRequest(
        Boolean voiceover_enabled,
        Integer caption_commit_ms,
        Integer font_size,
        Boolean high_contrast
) {}
package com.example.talaria.backend.dto.common;

import java.util.List;
import java.util.Map;

public record PageResponse(
        List<Map<String, Object>> items,
        int page,
        int size,
        long total
) {}

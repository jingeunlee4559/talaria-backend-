package com.example.talaria.backend.service.manner;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.manner.MannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MannerQueryService {

    private final MannerMapper mapper;

    private static final String TABLE = "tl_manner_dict";

    @Transactional(readOnly = true)
    public PageResponse list(Integer active,
                             List<String> agentCodes,
                             String speakerLocale,
                             String listenerLocale,
                             String culturePair,
                             String versionTag,
                             int page,
                             int size) {
        if (size <= 0) size = 50;
        if (size > 200) size = 200;
        if (page < 0) page = 0;
        int offset = page * size;

        long total = mapper.countRules(TABLE, active, agentCodes, speakerLocale, listenerLocale, culturePair, versionTag);
        var items = mapper.selectRules(TABLE, active, agentCodes, speakerLocale, listenerLocale, culturePair, versionTag, size, offset);
        return new PageResponse(items, page, size, total);
    }
}

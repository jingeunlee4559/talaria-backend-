package com.example.talaria.backend.service.manner;

import com.example.talaria.backend.mapper.manner.MannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MannerEventService {

    private final MannerMapper mapper;

    @Transactional
    public boolean checkAndMark(long ruleId, Long sessionId, String speakerId, int cooldownSec) {
        int affected = mapper.checkAndMarkEvent(ruleId, sessionId, speakerId, cooldownSec);
        // MySQL ON DUP KEY: insert=1, changed update=2, no change=0
        return affected > 0;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findEvent(long ruleId, Long sessionId, String speakerId) {
        return mapper.findEvent(ruleId, sessionId, speakerId);
    }
}

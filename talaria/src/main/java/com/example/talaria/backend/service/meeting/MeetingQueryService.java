package com.example.talaria.backend.service.meeting;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.meeting.MeetingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingQueryService {

    private final MeetingMapper meetingMapper;

    @Transactional(readOnly = true)
    public PageResponse listByMode(String mode, String status, Long hostId, int page, int size) {
        if (mode == null || mode.isBlank()) throw new IllegalArgumentException("mode is required (B or C)");
        String normMode = mode.trim().toUpperCase();
        if (!normMode.equals("B") && !normMode.equals("C")) {
            throw new IllegalArgumentException("mode must be 'B' or 'C'");
        }

        if (size <= 0) size = 20;
        if (size > 100) size = 100;
        if (page < 0) page = 0;
        int offset = page * size;

        long total = meetingMapper.countByMode(normMode, status, hostId);
        var items = meetingMapper.selectPageByMode(normMode, status, hostId, size, offset);
        return new PageResponse(items, page, size, total);
    }
}

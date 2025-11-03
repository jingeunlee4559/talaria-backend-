package com.example.talaria.backend.service.meeting;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.meeting.ParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantMapper mapper;

    // 목록
    public PageResponse list(long meetingsId, int page, int size) {
        if (size > 100) size = 100;
        int offset = page * size;
        long total = mapper.countByMeeting(meetingsId);
        List<Map<String,Object>> items = mapper.selectByMeeting(meetingsId, size, offset);
        return new PageResponse(items, page, size, total);
    }

    // 단건 조회
    public Map<String,Object> getOne(long meetingsId, long userId) {
        return mapper.selectOne(meetingsId, userId);
    }

    // 초대(역할 upsert)
    @Transactional
    public Map<String,Object> invite(long meetingsId, long userId, String role) {
        String normalized = normalizeRole(role);
        mapper.upsertRole(meetingsId, userId, normalized);
        return mapper.selectOne(meetingsId, userId);
    }

    // 입장(join) — 없으면 역할 기본 GUEST로 생성 후 join
    @Transactional
    public Map<String,Object> joined(long meetingsId, long userId, String roleOrNull) {
        String role = (roleOrNull == null) ? "GUEST" : normalizeRole(roleOrNull);
        mapper.upsertRole(meetingsId, userId, role);
        mapper.markJoined(meetingsId, userId);
        return mapper.selectOne(meetingsId, userId);
    }

    // 퇴장(leave)
    @Transactional
    public Map<String,Object> left(long meetingsId, long userId) {
        mapper.markLeft(meetingsId, userId);
        return mapper.selectOne(meetingsId, userId);
    }

    // 필요시 삭제
    @Transactional
    public int remove(long meetingsId, long userId) {
        return mapper.deleteOne(meetingsId, userId);
    }

    private static String normalizeRole(String role) {
        if (role == null) return "GUEST";
        String r = role.trim().toUpperCase();
        return switch (r) {
            case "HOST", "GUEST", "VIEWER" -> r;
            default -> "GUEST";
        };
    }
}

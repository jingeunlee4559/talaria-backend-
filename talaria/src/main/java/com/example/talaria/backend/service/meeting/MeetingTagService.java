package com.example.talaria.backend.service.meeting;

import com.example.talaria.backend.mapper.meeting.MeetingTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MeetingTagService {

    private final MeetingTagMapper mapper;

    /* ===== 조회 ===== */

    /** 마스터 태그 전체 */
    @Transactional(readOnly = true)
    public List<Map<String,Object>> listMasterTags(boolean activeOnly) {
        return mapper.selectAllMasterTags(activeOnly);
    }

    /** 특정 회의에 부착된 태그(상세) */
    @Transactional(readOnly = true)
    public List<Map<String,Object>> listAssigned(long meetingId) {
        return mapper.selectAssignedTags(meetingId);
    }

    /** 마스터 + 해당 회의 부착여부(assigned 플래그) */
    @Transactional(readOnly = true)
    public List<Map<String,Object>> listMasterWithAssigned(long meetingId, boolean activeOnly) {
        return mapper.selectMasterWithAssigned(meetingId, activeOnly);
    }

    /** 특정 회의에 부착된 tag_id 목록만 심플 조회 */
    @Transactional(readOnly = true)
    public List<Long> listAssignedIds(long meetingId) {
        List<Long> ids = mapper.selectAssignedTagIds(meetingId);
        return (ids == null) ? List.of() : ids;
    }

    /* ===== 단건 첨부/해제 ===== */

    @Transactional
    public Map<String,Object> attach(long userId, long meetingId, long tagId) {
        var meta = mapper.selectMeetingMeta(meetingId);
        if (meta == null) throw new IllegalArgumentException("회의를 찾을 수 없습니다.");
        // 필요 시 status/권한 체크 추가

        mapper.upsertMapping(meetingId, tagId, userId);
        return Map.of("attached", true, "meeting_id", meetingId, "tag_id", tagId);
    }

    @Transactional
    public Map<String,Object> detach(long meetingId, long tagId) {
        int n = mapper.deleteMapping(meetingId, tagId);
        return Map.of("deleted", n, "meeting_id", meetingId, "tag_id", tagId);
    }

    /* ===== 벌크 교체/초기화 ===== */

    /**
     * 회의 태그를 전달된 tagIds로 완전 교체
     * - before/after/added/removed/unchanged 세트 리포트 포함
     */
    @Transactional
    public Map<String,Object> replaceAll(long userId, long meetingId, List<Long> tagIds) {
        var meta = mapper.selectMeetingMeta(meetingId);
        if (meta == null) throw new IllegalArgumentException("회의를 찾을 수 없습니다.");

        // null 허용 → 빈 목록 처리
        List<Long> desired = (tagIds == null) ? List.of() : tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 기존 부착 목록
        List<Long> beforeList = listAssignedIds(meetingId);
        Set<Long> before = new LinkedHashSet<>(beforeList);
        Set<Long> desiredSet = new LinkedHashSet<>(desired);

        // 차집합/교집합 계산
        Set<Long> toRemove = new LinkedHashSet<>(before);
        toRemove.removeAll(desiredSet);

        Set<Long> toAdd = new LinkedHashSet<>(desiredSet);
        toAdd.removeAll(before);

        Set<Long> unchanged = new LinkedHashSet<>(before);
        unchanged.retainAll(desiredSet);

        // 1) 전달된 목록에 없는 매핑 제거
        mapper.deleteMappingsNotIn(meetingId, desired.isEmpty() ? List.of() : desired);

        // 2) 전달된 목록 모두 upsert
        int upserts = 0;
        for (Long tagId : desired) {
            upserts += mapper.upsertMapping(meetingId, tagId, userId);
        }

        // after 목록(정합성 확인 겸)
        List<Long> afterList = listAssignedIds(meetingId);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("meeting_id", meetingId);
        result.put("requested_count", desired.size());
        result.put("upsert_calls", upserts);
        result.put("before", beforeList);
        result.put("after", afterList);
        result.put("added", new ArrayList<>(toAdd));
        result.put("removed", new ArrayList<>(toRemove));
        result.put("unchanged", new ArrayList<>(unchanged));
        return result;
    }

    /** 회의의 모든 태그 매핑 제거(초기화) */
    @Transactional
    public Map<String,Object> clearAll(long meetingId) {
        int deleted = mapper.deleteAllByMeeting(meetingId);
        return Map.of("cleared", deleted, "meeting_id", meetingId);
    }
}

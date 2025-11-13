package com.example.talaria.backend.mapper.meeting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MeetingTagMapper {

    /** 회의 메타(권한/상태 확인 등) */
    Map<String,Object> selectMeetingMeta(@Param("meetingId") long meetingId);

    /** 마스터 태그 목록 */
    List<Map<String,Object>> selectAllMasterTags(@Param("activeOnly") boolean activeOnly);

    /** 특정 회의에 부착된 태그(상세) */
    List<Map<String,Object>> selectAssignedTags(@Param("meetingId") long meetingId);

    /** 단건 첨부(idempotent) */
    int upsertMapping(@Param("meetingId") long meetingId,
                      @Param("tagId") long tagId,
                      @Param("userId") long userId);

    /** 단건 해제 */
    int deleteMapping(@Param("meetingId") long meetingId,
                      @Param("tagId") long tagId);

    /** 전달된 목록 외 매핑 제거(벌크 교체용) */
    int deleteMappingsNotIn(@Param("meetingId") long meetingId,
                            @Param("tagIds") List<Long> tagIds);

    /* ===== 추가: 선택지+부착여부 플래그/심플ID/전체삭제 ===== */

    /** 마스터 + 해당 회의 부착여부(assigned 플래그) */
    List<Map<String,Object>> selectMasterWithAssigned(@Param("meetingId") long meetingId,
                                                      @Param("activeOnly") boolean activeOnly);

    /** 특정 회의에 부착된 tag_id 목록(심플) */
    List<Long> selectAssignedTagIds(@Param("meetingId") long meetingId);

    /** 회의의 모든 태그 매핑 전체 삭제 */
    int deleteAllByMeeting(@Param("meetingId") long meetingId);
}

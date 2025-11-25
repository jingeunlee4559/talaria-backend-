package com.example.talaria.backend.mapper.meeting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MeetingSummaryMapper {

    /**
     * tl_meeting_summaries 에서 meeting_id + lang 으로 1건 조회
     */
    Map<String, Object> selectByMeetingAndLang(
            @Param("meetingId") long meetingId,
            @Param("lang") String lang
    );

    List<Map<String, Object>> selectAllByUserAndLang(@Param("userId") long userId,
                                                 @Param("lang") String lang);
}

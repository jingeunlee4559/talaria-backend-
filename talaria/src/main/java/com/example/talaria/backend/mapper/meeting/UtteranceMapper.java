package com.example.talaria.backend.mapper.meeting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UtteranceMapper {

    /**
     * 회의별 발화 조회 (회의 스크립트용)
     */
    List<Map<String, Object>> selectByMeetingForScript(
            @Param("meetingId") long meetingId
    );

     List<Map<String, Object>> selectByMeetingForApi(@Param("meetingId") long meetingId);
}

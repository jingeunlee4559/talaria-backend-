package com.example.talaria.backend.mapper.meeting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ParticipantMapper {

    // 회의별 목록
    List<Map<String,Object>> selectByMeeting(@Param("meetingsId") long meetingsId,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    long countByMeeting(@Param("meetingsId") long meetingsId);
    

    // 단건 조회
    Map<String,Object> selectOne(@Param("meetingsId") long meetingsId,
                                 @Param("userId") long userId);

    // 역할 upsert (초대 개념: joined_at/left_at은 건드리지 않음)
    int upsertRole(@Param("meetingsId") long meetingsId,
                   @Param("userId") long userId,
                   @Param("role") String role);

    // JOIN (입장 기록: joined_at=NOW(), left_at=NULL)
    int markJoined(@Param("meetingsId") long meetingsId,
                   @Param("userId") long userId);

    // LEAVE (퇴장 기록: left_at=NOW())
    int markLeft(@Param("meetingsId") long meetingsId,
                 @Param("userId") long userId);

    // 행 삭제(옵션)
    int deleteOne(@Param("meetingsId") long meetingsId,
                  @Param("userId") long userId);

   List<Map<String,Object>> selectByMeetingWithUserInfo(@Param("meetingsId") long meetingsId);

    long countByMeetingIncludingHost(@Param("meetingsId") long meetingsId);
    
}

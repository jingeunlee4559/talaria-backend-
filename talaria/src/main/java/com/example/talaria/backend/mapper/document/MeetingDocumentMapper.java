package com.example.talaria.backend.mapper.document;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MeetingDocumentMapper {

    String selectMeetingStatus(@Param("meetingId") long meetingId);

    List<Map<String, Object>> selectDocsByMeeting(@Param("meetingId") long meetingId);

    int insertMeetingDoc(@Param("meetingId") long meetingId,
                         @Param("docId") long docId,
                         @Param("addedByUserId") long addedByUserId);

    int deleteMeetingDoc(@Param("meetingId") long meetingId,
                         @Param("docId") long docId);

    Long countDocOwnedByUser(@Param("docId") long docId,
                             @Param("userId") long userId);
}

package com.example.talaria.backend.mapper.document;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MeetingDocumentMapper {

    String selectMeetingStatus(@Param("meetingId") long meetingId);

    List<Map<String, Object>> selectDocsByMeeting(@Param("meetingId") long meetingId);

    int insertMeetingDoc(@Param("meetingId") long meetingId,
                         @Param("docId") long docId,
                         @Param("addedByUserId") long addedByUserId);

    int deleteMeetingDoc(@Param("meetingId") long meetingId,
                         @Param("docId") long docId);

    long countDocOwnedByUser(@Param("docId") long docId,
                             @Param("userId") long userId);

    // ===== 신규(권한 포함 목록/다운로드) =====
    long countByUserAndMeeting(@Param("userId") long userId,
                               @Param("meetingId") long meetingId);

    List<Map<String, Object>> selectPageByUserAndMeeting(@Param("userId") long userId,
                                                         @Param("meetingId") long meetingId,
                                                         @Param("limit") int limit,
                                                         @Param("offset") int offset);

    Map<String, Object> selectOneForDownload(@Param("userId") long userId,
                                             @Param("meetingId") long meetingId,
                                             @Param("docId") long docId);
}

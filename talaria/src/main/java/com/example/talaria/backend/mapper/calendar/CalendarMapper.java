package com.example.talaria.backend.mapper.calendar;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CalendarMapper {
    
    // 조회 관련
    long countByUser(@Param("table") String table,
                     @Param("pk") String pk,
                     @Param("userId") long userId,
                     @Param("eventId") Long eventId,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

    List<Map<String, Object>> selectPageByUser(@Param("table") String table,
                                               @Param("pk") String pk,
                                               @Param("userId") long userId,
                                               @Param("eventId") Long eventId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    // 추가 관련
    int insertEvent(@Param("table") String table,
                    @Param("event") Map<String, Object> event);

    // 단일 조회
    Map<String, Object> selectOneById(@Param("table") String table,
                                     @Param("pk") String pk,
                                     @Param("eventId") long eventId,
                                     @Param("userId") long userId);

    //  수정
    int updateEvent(@Param("table") String table,
                    @Param("pk") String pk,
                    @Param("event") Map<String, Object> event);

    //  삭제
    int deleteEvent(@Param("table") String table,
                    @Param("pk") String pk,
                    @Param("userId") long userId,
                    @Param("eventId") long eventId);

    // 회의 연동 일정 전체 업데이트(회의 시간/제목 변경 시)
    int updateAllByMeetingId(@org.apache.ibatis.annotations.Param("table") String table,
                             @org.apache.ibatis.annotations.Param("pk") String pk,
                             @org.apache.ibatis.annotations.Param("meetingId") Long meetingId,
                             @org.apache.ibatis.annotations.Param("title") String title,
                             @org.apache.ibatis.annotations.Param("startDate") java.time.LocalDate startDate,
                             @org.apache.ibatis.annotations.Param("endDate") java.time.LocalDate endDate,
                             @org.apache.ibatis.annotations.Param("startTime") java.time.LocalTime startTime,
                             @org.apache.ibatis.annotations.Param("endTime") java.time.LocalTime endTime,
                             @org.apache.ibatis.annotations.Param("location") String location,
                             @org.apache.ibatis.annotations.Param("description") String description);

    // 회의 연동 일정 전체 삭제(회의 취소/삭제 시)
    int deleteByMeetingId(@org.apache.ibatis.annotations.Param("table") String table,
                          @org.apache.ibatis.annotations.Param("pk") String pk,
                          @org.apache.ibatis.annotations.Param("meetingId") Long meetingId);

    // 특정 사용자-회의 연동 일정 삭제(참가자 제거 시 또는 upsert 사전 정리용)
    int deleteByUserAndMeeting(@org.apache.ibatis.annotations.Param("table") String table,
                               @org.apache.ibatis.annotations.Param("pk") String pk,
                               @org.apache.ibatis.annotations.Param("userId") Long userId,
                               @org.apache.ibatis.annotations.Param("meetingId") Long meetingId);
}
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
}
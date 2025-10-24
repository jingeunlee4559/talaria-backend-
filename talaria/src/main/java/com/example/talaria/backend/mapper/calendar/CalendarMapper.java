package com.example.talaria.backend.mapper.calendar;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CalendarMapper {
  long countByUser(@Param("table") String table,
                   @Param("pk")    String pk,
                   @Param("userId") long userId,
                   @Param("eventId") Long eventId,
                   @Param("from") java.time.LocalDateTime from,
                   @Param("to")   java.time.LocalDateTime to);

  List<Map<String, Object>> selectPageByUser(@Param("table") String table,
                                             @Param("pk")    String pk,
                                             @Param("userId") long userId,
                                             @Param("eventId") Long eventId,
                                             @Param("from") java.time.LocalDateTime from,
                                             @Param("to")   java.time.LocalDateTime to,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);
}
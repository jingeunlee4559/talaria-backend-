package com.example.talaria.backend.mapper.alert;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AlertMapper {

    long countByUser(@Param("table") String table,
                     @Param("pk") String pk,
                     @Param("userId") long userId,
                     @Param("alertId") Long alertId,
                     @Param("status") String status);

    List<Map<String, Object>> selectPageByUser(@Param("table") String table,
                                               @Param("pk") String pk,
                                               @Param("userId") long userId,
                                               @Param("alertId") Long alertId,
                                               @Param("status") String status,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    int insert(@Param("table") String table,
               @Param("userId") long userId,
               @Param("type") String type,
               @Param("title") String title,
               @Param("body") String body,
               @Param("payload") String payloadJson);

    long lastInsertId();

    int markDelivered(@Param("table") String table,
                      @Param("userId") long userId,
                      @Param("id") long id,
                      @Param("now") LocalDateTime now);

    int markSeen(@Param("table") String table,
                 @Param("userId") long userId,
                 @Param("id") long id,
                 @Param("now") LocalDateTime now);

    int markAllSeen(@Param("table") String table,
                    @Param("userId") long userId,
                    @Param("now") LocalDateTime now);
}

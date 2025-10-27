package com.example.talaria.backend.mapper.alert;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AlertMapper {

    // ===== 조회 =====
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

    Map<String, Object> selectOneByUser(@Param("table") String table,
                                        @Param("pk") String pk,
                                        @Param("userId") long userId,
                                        @Param("id") long id);

    // ===== 생성 =====
    int insert(@Param("table") String table,
               @Param("userId") long userId,
               @Param("type") String type,
               @Param("title") String title,
               @Param("body") String body,
               @Param("payloadJson") String payloadJson);

    long lastInsertId();

    // ===== 상태 전이 =====
    int markDelivered(@Param("table") String table,
                    @Param("pk") String pk,
                    @Param("userId") long userId,
                    @Param("id") long id,
                    @Param("now") LocalDateTime now);

    // seen
    int markSeen(@Param("table") String table,
                @Param("pk") String pk,
                @Param("userId") long userId,
                @Param("id") long id,
                @Param("now") LocalDateTime now);

    // 유저 전체 미열람 -> SEEN
    int markAllSeen(@Param("table") String table,
                    @Param("userId") long userId,
                    @Param("now") LocalDateTime now);

    // (선택) 배지용 미열람 카운트
    long countUnreadByUser(@Param("table") String table,
                           @Param("userId") long userId);
}

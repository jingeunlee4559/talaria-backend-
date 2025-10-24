// src/main/java/com/example/talaria/backend/mapper/todo/TodoMapper.java
package com.example.talaria.backend.mapper.todo;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TodoMapper {
    long countByUser(@Param("userId") long userId,
                     @Param("status") String status,
                     @Param("from") LocalDate from,
                     @Param("to") LocalDate to);

    List<Map<String, Object>> selectPageByUser(@Param("userId") long userId,
                                               @Param("status") String status,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    Map<String, Object> selectOneByUser(@Param("userId") long userId,
                                        @Param("taskId") long taskId);
}

// src/main/java/com/example/talaria/backend/service/todo/TodoCommandService.java
package com.example.talaria.backend.service.todo;

import com.example.talaria.backend.mapper.todo.TodoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TodoCommandService {
    private final TodoMapper mapper;

    @Transactional
    public Map<String, Object> create(long userId,
                                      String title,
                                      String description,
                                      String status,            // null -> pending
                                      LocalTime dueTime,
                                      LocalDate dueDate,
                                      String colorCode,
                                      Integer displayOrder,
                                      Long meetingId) {

        String st = (status == null || status.isBlank()) ? "pending" : status;
        Long completedAt = "completed".equals(st) ? System.currentTimeMillis() : null;

        mapper.insert(
            userId, title, description, st,
            dueTime, dueDate, colorCode, displayOrder,
            completedAt, meetingId
        );
        long id = mapper.lastInsertId();
        return mapper.selectOneByUser(userId, id);
    }

    @Transactional
    public int delete(long userId, long taskId) {
        return mapper.deleteByUser(userId, taskId);
    }

    public int updateStatus(long userId, long taskId, String status) {
        if (!"pending".equals(status) && !"completed".equals(status)) {
            throw new IllegalArgumentException("status must be 'pending' or 'completed'");
        }
        return mapper.updateStatus(userId, taskId, status);
    }
    @Transactional
    public int updatePartial(
            long userId, long taskId,
            String title,
            String description,
            String status,
            LocalTime dueTime,
            LocalDate dueDate,
            String colorCode,
            Integer displayOrder,
            Long meetingId
    ) {
        return mapper.updatePartial(
            userId, taskId,
            title, description, status,
            dueTime, dueDate, colorCode, displayOrder, meetingId
        );
    }
}

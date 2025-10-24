// src/main/java/com/example/talaria/backend/service/todo/TodoQueryService.java
package com.example.talaria.backend.service.todo;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.todo.TodoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TodoQueryService {
    private final TodoMapper mapper;

    public PageResponse listByUser(long userId, int page, int size, String status, LocalDate from, LocalDate to) {
        if (size > 100) size = 100;
        int offset = page * size;
        long total = mapper.countByUser(userId, status, from, to);
        List<Map<String, Object>> items = mapper.selectPageByUser(userId, status, from, to, size, offset);
        return new PageResponse(items, page, size, total);
    }

    public Map<String, Object> getOneByUser(long userId, long taskId) {
        var row = mapper.selectOneByUser(userId, taskId);
        if (row == null || row.isEmpty()) throw new RuntimeException("Todo not found or not owned by user");
        return row;
    }
}

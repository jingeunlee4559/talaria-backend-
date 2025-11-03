package com.example.talaria.backend.service.todo;

import com.example.talaria.backend.mapper.todo.TodoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoCommandService {

    private final TodoMapper mapper;

    @Transactional
    public int delete(long userId, long taskId) {
        return mapper.deleteOne(taskId, userId);
    }

    @Transactional
    public int changeStatus(long userId, long taskId, String status) {
        // 허용값 정규화
        String s = ("completed".equalsIgnoreCase(status)) ? "completed" : "pending";
        return mapper.updateStatus(taskId, userId, s);
    }    
}

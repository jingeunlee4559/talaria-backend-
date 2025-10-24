package com.example.talaria.backend.service.todo;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.todo.TodoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TodoQueryService {
    private final TodoMapper mapper;

    // 🔧 설계서 값으로 교체
    private static final String TABLE = "tl_todo_tasks"; // 실제 테이블명
    private static final String PK    = "task_id";       // 실제 PK 컬럼명

    public Map<String, Object> findById(long id) {
        var row = mapper.selectById(TABLE, PK, id);
        if (row == null || row.isEmpty()) throw new RuntimeException("Todo not found: id=" + id);
        return row;
    }

    public PageResponse findPage(int page, int size) {
        if (size > 100) size = 100;
        int offset = page * size;
        long total = mapper.count(TABLE);
        List<Map<String, Object>> items = mapper.selectPage(TABLE, PK, size, offset);
        return new PageResponse(items, page, size, total);
    }
}

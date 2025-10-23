package com.example.talaria.backend.service.user;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.mapper.user.UserMapper;
import com.example.talaria.backend.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserQueryService {
    private final UserMapper mapper;

    public Map<String, Object> findById(long id) {
        var row = mapper.selectById(id);
        if (row == null || row.isEmpty()) throw new UserNotFoundException(id);
        return row;
    }

    public PageResponse findPage(int page, int size) {
        if (size > 100) size = 100;
        int offset = page * size;
        List<Map<String, Object>> items = mapper.selectPage(size, offset);
        long total = mapper.countAll();
        return new PageResponse(items, page, size, total);
    }
}

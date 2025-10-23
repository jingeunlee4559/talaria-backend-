package com.example.talaria.backend.service.user;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.repository.user.UserJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserJdbcRepository repo;

    public Map<String, Object> findById(long id) {
        return repo.findById(id);
    }

    public PageResponse findPage(int page, int size) {
        if (size > 100) size = 100;
        Assert.isTrue(page >= 0, "page must be >= 0");
        Assert.isTrue(size > 0, "size must be > 0");

        long total = repo.countAll();
        int offset = page * size;
        List<Map<String, Object>> items = repo.findPage(size, offset);
        return new PageResponse(items, page, size, total);
    }
}

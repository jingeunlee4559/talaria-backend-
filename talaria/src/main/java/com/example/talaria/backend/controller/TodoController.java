package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.todo.TodoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/todos")
public class TodoController {

    private final TodoQueryService service;

    @GetMapping
    public ResponseEntity<PageResponse> listByUser(@PathVariable long userId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) LocalDate due_from,
                                                   @RequestParam(required = false) LocalDate due_to) {
        return ResponseEntity.ok(service.listByUser(userId, page, size, status, due_from, due_to));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable long userId,
                                                      @PathVariable long taskId) {
        return ResponseEntity.ok(service.getOneByUser(userId, taskId));
    }
}

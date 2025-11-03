// src/main/java/com/example/talaria/backend/controller/TodoController.java
package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.todo.TodoCommandService;
import com.example.talaria.backend.service.todo.TodoQueryService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/todos")
public class TodoController {

    private final TodoQueryService queryService;     // 기존 조회 유지
    private final TodoCommandService commandService; // 추가: 삭제/상태변경

    // === 조회(기존 유지) ===
    @GetMapping
    public ResponseEntity<PageResponse> listByUser(@PathVariable long userId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) LocalDate due_from,
                                                   @RequestParam(required = false) LocalDate due_to) {
        return ResponseEntity.ok(queryService.listByUser(userId, page, size, status, due_from, due_to));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable long userId,
                                                      @PathVariable long taskId) {
        return ResponseEntity.ok(queryService.getOneByUser(userId, taskId));
    }

    // === 상태 변경 ===
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<?> changeStatus(@PathVariable long userId,
                                          @PathVariable long taskId,
                                          @RequestBody ChangeStatusReq req) {
        try {
            int n = commandService.changeStatus(userId, taskId, req.getStatus());
            return ResponseEntity.ok(Map.of("updated", n));
        } catch (Exception e) {
            var pd = ProblemDetail.forStatus(500);
            pd.setTitle("Database Error");
            pd.setDetail("Database error");
            pd.setInstance(URI.create("/api/users/" + userId + "/todos/" + taskId + "/status"));
            return ResponseEntity.status(500).body(Map.of(
                "type","about:blank","title",pd.getTitle(),"status",500,
                "detail",pd.getDetail(),"instance",pd.getInstance().toString()
            ));
        }
    }

    // === 삭제 ===
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> delete(@PathVariable long userId,
                                    @PathVariable long taskId) {
        try {
            int n = commandService.delete(userId, taskId);
            return ResponseEntity.ok(Map.of("deleted", n));
        } catch (Exception e) {
            var pd = ProblemDetail.forStatus(500);
            pd.setTitle("Database Error");
            pd.setDetail("Database error");
            pd.setInstance(URI.create("/api/users/" + userId + "/todos/" + taskId));
            return ResponseEntity.status(500).body(Map.of(
                "type","about:blank","title",pd.getTitle(),"status",500,
                "detail",pd.getDetail(),"instance",pd.getInstance().toString()
            ));
        }
    }

    // === DTO ===
    @Data
    public static class ChangeStatusReq {
        @NotBlank
        private String status; // 'pending' | 'completed'
    }
}

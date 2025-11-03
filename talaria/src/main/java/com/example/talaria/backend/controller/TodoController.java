package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.todo.TodoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/todos")
public class TodoController {

    private final TodoQueryService service;

    @Operation(
        summary = "사용자 할 일 목록 조회",
        description = "사용자별 할 일 목록을 페이지네이션/필터(상태, 마감일 범위)로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<PageResponse> listByUser(
            @Parameter(
                name = "userId",
                description = "사용자 ID",
                example = "1"
            )
            @PathVariable long userId,

            @Parameter(
                name = "page",
                description = "페이지 번호(0부터 시작)",
                example = "0",
                schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")
            )
            @RequestParam(name = "page", defaultValue = "0") int page,

            @Parameter(
                name = "size",
                description = "페이지 크기(최대 100)",
                example = "20",
                schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100")
            )
            @RequestParam(name = "size", defaultValue = "20") int size,

            @Parameter(
                name = "status",
                description = "상태값 (pending|completed)",
                example = "pending",
                schema = @Schema(allowableValues = {"pending", "completed"})
            )
            @RequestParam(name = "status", required = false) String status,

            @Parameter(
                name = "due_from",
                description = "마감일 시작(포함)",
                example = "2025-11-01",
                schema = @Schema(type = "string", format = "date")
            )
            @RequestParam(name = "due_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,

            @Parameter(
                name = "due_to",
                description = "마감일 끝(포함)",
                example = "2025-11-30",
                schema = @Schema(type = "string", format = "date")
            )
            @RequestParam(name = "due_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo
    ) {
        return ResponseEntity.ok(service.listByUser(userId, page, size, status, dueFrom, dueTo));
    }

    @Operation(summary = "할 일 단건 조회", description = "사용자 소유의 특정 할 일을 조회합니다.")
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getOne(
            @Parameter(name = "userId", description = "사용자 ID", example = "1")
            @PathVariable long userId,

            @Parameter(name = "taskId", description = "할 일 ID", example = "123")
            @PathVariable long taskId
    ) {
        return ResponseEntity.ok(service.getOneByUser(userId, taskId));
    }
}

package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.todo.TodoCommandService;
import com.example.talaria.backend.service.todo.TodoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Todos", description = "할 일(Task) 조회/삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/todos")
public class TodoController {

    private final TodoQueryService queryService;
    private final TodoCommandService commandService;

    @Operation(
        summary = "사용자별 Todo 목록 조회",
        description = "상태/기간 필터와 페이지네이션으로 Todo 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<PageResponse> listByUser(
            @Parameter(name = "userId", description = "사용자 ID", in = ParameterIn.PATH, required = true,
                       schema = @Schema(type = "integer", format = "int64"), example = "1")
            @PathVariable long userId,

            @Parameter(name = "page", description = "페이지 번호(0부터 시작)", in = ParameterIn.QUERY,
                       schema = @Schema(type = "integer", format = "int32", defaultValue = "0", minimum = "0"),
                       example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,

            @Parameter(name = "size", description = "페이지 크기(최대 100)", in = ParameterIn.QUERY,
                       schema = @Schema(type = "integer", format = "int32", defaultValue = "20", minimum = "1", maximum = "100"),
                       example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size,

            @Parameter(name = "status", description = "상태값", in = ParameterIn.QUERY,
                       schema = @Schema(allowableValues = {"pending","completed"}), example = "pending")
            @RequestParam(name = "status", required = false) String status,

            @Parameter(name = "due_from", description = "마감일 시작(포함)", in = ParameterIn.QUERY,
                       schema = @Schema(type = "string", format = "date"), example = "2025-11-01")
            @RequestParam(name = "due_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,

            @Parameter(name = "due_to", description = "마감일 끝(포함)", in = ParameterIn.QUERY,
                       schema = @Schema(type = "string", format = "date"), example = "2025-11-30")
            @RequestParam(name = "due_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo
    ) {
        return ResponseEntity.ok(queryService.listByUser(userId, page, size, status, dueFrom, dueTo));
    }

    @Operation(summary = "단건 조회", description = "사용자의 단일 Todo를 조회합니다.")
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getOne(
            @Parameter(name = "userId", description = "사용자 ID", in = ParameterIn.PATH, required = true,
                       schema = @Schema(type = "integer", format = "int64"), example = "1")
            @PathVariable long userId,

            @Parameter(name = "taskId", description = "할 일 ID", in = ParameterIn.PATH, required = true,
                       schema = @Schema(type = "integer", format = "int64"), example = "101")
            @PathVariable long taskId
    ) {
        return ResponseEntity.ok(queryService.getOneByUser(userId, taskId));
    }

    @Operation(summary = "삭제", description = "사용자의 Todo를 삭제합니다.")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> delete(
            @Parameter(name = "userId", description = "사용자 ID", in = ParameterIn.PATH, required = true,
                       schema = @Schema(type = "integer", format = "int64"), example = "1")
            @PathVariable long userId,

            @Parameter(name = "taskId", description = "할 일 ID", in = ParameterIn.PATH, required = true,
                       schema = @Schema(type = "integer", format = "int64"), example = "101")
            @PathVariable long taskId
    ) {
        int n = commandService.delete(userId, taskId);
        return ResponseEntity.ok(Map.of("deleted", n));
    }
}

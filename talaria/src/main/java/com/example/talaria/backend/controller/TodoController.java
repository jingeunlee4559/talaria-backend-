// src/main/java/com/example/talaria/backend/controller/TodoController.java
package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.todo.TodoCommandService;
import com.example.talaria.backend.service.todo.TodoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Tag(name = "Todos", description = "할 일(Task) 조회/생성/삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/todos")
public class TodoController {

    private final TodoQueryService queryService;
    private final TodoCommandService commandService;

    // ====== 목록 조회 ======
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

    // ====== 단건 조회 ======
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

    // ====== 생성(POST) ======
    @Operation(
        summary = "Todo 생성",
        description = "할 일 내용/시간/색상 등을 입력받아 Todo를 생성합니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                schema = @Schema(implementation = CreateTodoReq.class),
                examples = {
                    @ExampleObject(
                        name = "기본 예시",
                        value = """
                                {
                                  "title": "회의자료 정리",
                                  "description": "회의 메모를 Notion에 정리",
                                  "dueDate": "2025-11-15",
                                  "dueTime": "14:00:00",
                                  "colorCode": "#FF6B6B",
                                  "displayOrder": 10,
                                  "meetingId": 21
                                }
                                """
                    )
                }
            )
        )
    )
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Parameter(name = "userId", description = "사용자 ID", in = ParameterIn.PATH, required = true,
                       schema = @Schema(type = "integer", format = "int64"), example = "1")
            @PathVariable long userId,
            @Valid @RequestBody CreateTodoReq body
    ) {
        var created = commandService.create(
            userId,
            body.getTitle(),
            body.getDescription(),
            body.getStatus(),      // null 이면 pending 으로 처리
            body.getDueTime(),
            body.getDueDate(),
            body.getColorCode(),
            body.getDisplayOrder(),
            body.getMeetingId()
        );
        return ResponseEntity.ok(created);
    }

    // ====== 삭제(DELETE) ======
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
    @Operation(summary = "할 일 상태 변경", description = "pending 또는 completed 로 상태를 변경합니다.")
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @PathVariable long userId,
            @PathVariable long taskId,
            @RequestBody StatusPatchReq req) {
        int updated = commandService.updateStatus(userId, taskId, req.getStatus());
        return ResponseEntity.ok(Map.of("updated", updated));
    }
    @Data
    public static class StatusPatchReq {
        @NotBlank
        @Pattern(regexp = "^(pending|completed)$", message = "status는 pending|completed 만 허용")
        @Schema(description = "변경할 상태", allowableValues = {"pending","completed"}, example = "completed")
        private String status;
    }
    // ====== DTO ======
    @Data
    public static class CreateTodoReq {
        @NotBlank
        @Size(max = 128)
        @Schema(description = "할 일 제목", example = "회의자료 정리")
        private String title;

        @Schema(description = "상세 설명", example = "회의 메모를 Notion에 정리")
        private String description;

        @Pattern(regexp = "^(pending|completed)$", message = "status는 pending|completed 만 허용")
        @Schema(description = "상태값 (기본 pending)", allowableValues = {"pending","completed"}, example = "pending")
        private String status;

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(description = "마감일", type = "string", format = "date", example = "2025-11-15")
        private LocalDate dueDate;

        @DateTimeFormat(pattern = "HH:mm:ss")
        @Schema(description = "예정 시간(HH:mm:ss)", type = "string", example = "14:00:00")
        private LocalTime dueTime;

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상은 #RRGGBB 형식")
        @Schema(description = "UI 색상(#RRGGBB)", example = "#FF6B6B")
        private String colorCode;

        @Schema(description = "정렬 순서(작을수록 위)", example = "10")
        private Integer displayOrder;

        @Schema(description = "관련 회의 ID(옵션)", example = "21")
        private Long meetingId;
    }
}

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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Todo", description = "할 일(Todo) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/todos")
public class TodoController {

    private final TodoQueryService queryService;     // 조회
    private final TodoCommandService commandService; // 삭제/상태변경

    // === 조회 목록 ===
    @Operation(
        summary = "유저별 Todo 목록 조회",
        description = "상태/기간 필터와 페이지네이션으로 목록을 반환합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK")
        }
    )
    @GetMapping
    public ResponseEntity<PageResponse> listByUser(
            @Parameter(in = ParameterIn.PATH, description = "유저 ID", example = "1")
            @PathVariable long userId,

            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기(최대 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(
                description = "상태값 (pending|completed)",
                schema = @Schema(type = "string", allowableValues = {"pending","completed"}),
                example = "pending"
            )
            @RequestParam(required = false) String status,

            @Parameter(
                description = "마감일 시작(포함)",
                schema = @Schema(type = "string", format = "date", example = "2025-11-01")
            )
            @RequestParam(name = "due_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate due_from,

            @Parameter(
                description = "마감일 끝(포함)",
                schema = @Schema(type = "string", format = "date", example = "2025-11-30")
            )
            @RequestParam(name = "due_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate due_to
    ) {
        return ResponseEntity.ok(queryService.listByUser(userId, page, size, status, due_from, due_to));
    }

    // === 단건 조회 ===
    @Operation(summary = "Todo 단건 조회", responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found")
    })
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getOne(
            @Parameter(in = ParameterIn.PATH, description = "유저 ID", example = "1")
            @PathVariable long userId,

            @Parameter(in = ParameterIn.PATH, description = "할 일 ID", example = "123")
            @PathVariable long taskId
    ) {
        return ResponseEntity.ok(queryService.getOneByUser(userId, taskId));
    }

    // === 상태 변경 ===
    @Operation(
        summary = "Todo 상태 변경",
        requestBody = @RequestBody(
            required = true,
            description = "변경할 상태값",
            content = @Content(
                schema = @Schema(implementation = ChangeStatusReq.class),
                examples = {
                    @ExampleObject(name = "완료로 변경", value = "{ \"status\": \"completed\" }"),
                    @ExampleObject(name = "대기로 변경", value = "{ \"status\": \"pending\" }")
                }
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "DB 에러")
        }
    )
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<?> changeStatus(
            @Parameter(in = ParameterIn.PATH, description = "유저 ID", example = "1")
            @PathVariable long userId,

            @Parameter(in = ParameterIn.PATH, description = "할 일 ID", example = "123")
            @PathVariable long taskId,

            @org.springframework.web.bind.annotation.RequestBody ChangeStatusReq req
    ) {
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
    @Operation(
        summary = "Todo 삭제",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "DB 에러")
        }
    )
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> delete(
            @Parameter(in = ParameterIn.PATH, description = "유저 ID", example = "1")
            @PathVariable long userId,

            @Parameter(in = ParameterIn.PATH, description = "할 일 ID", example = "123")
            @PathVariable long taskId
    ) {
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
        @Schema(
            description = "상태값 (pending|completed)",
            allowableValues = {"pending","completed"},
            example = "completed",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        private String status;
    }
}

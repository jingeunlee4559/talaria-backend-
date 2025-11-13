package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.meeting.MeetingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingQueryController {

    private final MeetingQueryService service;

    /**
     * meeting_mode 별 조회 (B/C)
     * 예) GET /api/meetings/by-mode?mode=C&status=ONGOING&page=0&size=20
     *     GET /api/meetings/by-mode?mode=B&hostId=1
     */
    @GetMapping("/by-mode")
    public ResponseEntity<PageResponse> listByMode(
            @RequestParam("mode") String mode,                 // B or C (필수)
            @RequestParam(name = "status", required = false) String status, // SCHEDULED/ONGOING/ENDED (옵션)
            @RequestParam(name = "hostId", required = false) Long hostId,   // 호스트 ID 필터 (옵션)
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listByMode(mode, status, hostId, page, size));
    }
}

package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.common.PageResponse;
import com.example.talaria.backend.service.calendar.CalendarQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarQueryService service;

    @GetMapping("/events/{id}")
    public ResponseEntity<Map<String,Object>> getById(@PathVariable long id){
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/events")
    public ResponseEntity<PageResponse> getPage(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.findPage(page, size));
    }
}

package com.example.demo.controller;

import com.example.demo.dto.TimeOffDTO;
import com.example.demo.entity.TimeOff;
import com.example.demo.service.TimeOffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/time-off")
public class TimeOffController {

    private final TimeOffService service;

    public TimeOffController(TimeOffService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<TimeOffDTO>> getMine() {
        return ResponseEntity.ok(service.getMine());
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<TimeOffDTO> request(@Valid @RequestBody TimeOffDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.request(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TimeOffDTO>> findByStatus(
            @RequestParam(defaultValue = "PENDING") TimeOff.Status status) {
        return ResponseEntity.ok(service.findByStatus(status));
    }

    @GetMapping("/pending-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> pendingCount() {
        return ResponseEntity.ok(Map.of("count", service.countPending()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimeOffDTO> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body == null ? null : body.get("note");
        return ResponseEntity.ok(service.approve(id, note));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimeOffDTO> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body == null ? null : body.get("note");
        return ResponseEntity.ok(service.reject(id, note));
    }
}

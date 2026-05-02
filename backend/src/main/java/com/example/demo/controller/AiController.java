package com.example.demo.controller;

import com.example.demo.dto.ai.TriageRequest;
import com.example.demo.dto.ai.TriageResponse;
import com.example.demo.dto.ai.VisitSummaryRequest;
import com.example.demo.dto.ai.VisitSummaryResponse;
import com.example.demo.service.ai.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("enabled", aiService.isEnabled()));
    }

    /**
     * Patient-facing symptom triage. Returns a department suggestion + urgency
     * + a draft chief complaint they can paste into the booking form.
     */
    @PostMapping("/triage")
    public ResponseEntity<TriageResponse> triage(@Valid @RequestBody TriageRequest req) {
        return ResponseEntity.ok(aiService.triage(req));
    }

    /**
     * Doctor-only: turn clinical notes into a plain-language patient summary.
     * Pass {save: true} to persist it onto the medical record + notify the patient.
     */
    @PostMapping("/visit-summary")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<VisitSummaryResponse> visitSummary(@Valid @RequestBody VisitSummaryRequest req) {
        return ResponseEntity.ok(aiService.visitSummary(req));
    }
}

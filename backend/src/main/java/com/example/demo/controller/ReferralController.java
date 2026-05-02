package com.example.demo.controller;

import com.example.demo.dto.ReferralDTO;
import com.example.demo.entity.Referral;
import com.example.demo.service.ReferralService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/referrals")
public class ReferralController {

    private final ReferralService service;

    public ReferralController(ReferralService service) {
        this.service = service;
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<ReferralDTO>> incoming() {
        return ResponseEntity.ok(service.incoming());
    }

    @GetMapping("/outgoing")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<ReferralDTO>> outgoing() {
        return ResponseEntity.ok(service.outgoing());
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ReferralDTO>> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.forPatient(patientId));
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ReferralDTO> create(@Valid @RequestBody ReferralDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/{id}/status/{status}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ReferralDTO> setStatus(@PathVariable Long id, @PathVariable Referral.Status status) {
        return ResponseEntity.ok(service.setStatus(id, status));
    }
}

package com.example.demo.controller;

import com.example.demo.dto.PatientProfileDTO;
import com.example.demo.service.PatientProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/patient-profiles")
public class PatientProfileController {

    private final PatientProfileService service;

    public PatientProfileController(PatientProfileService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ResponseEntity<PatientProfileDTO> getMine() {
        return ResponseEntity.ok(service.getMine());
    }

    @PutMapping("/me")
    public ResponseEntity<PatientProfileDTO> updateMine(@Valid @RequestBody PatientProfileDTO dto) {
        return ResponseEntity.ok(service.updateMine(dto));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<PatientProfileDTO> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }
}

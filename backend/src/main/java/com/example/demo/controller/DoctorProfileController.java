package com.example.demo.controller;

import com.example.demo.dto.DoctorProfileDTO;
import com.example.demo.service.DoctorProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctor-profiles")
public class DoctorProfileController {

    private final DoctorProfileService service;

    public DoctorProfileController(DoctorProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DoctorProfileDTO>> findAll() {
        return ResponseEntity.ok(service.findAllPublic());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorProfileDTO> getMine() {
        return ResponseEntity.ok(service.getMine());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorProfileDTO> updateMine(@Valid @RequestBody DoctorProfileDTO dto) {
        return ResponseEntity.ok(service.updateMine(dto));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<DoctorProfileDTO> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorProfileDTO> adminUpsert(
            @PathVariable Long userId,
            @Valid @RequestBody DoctorProfileDTO dto
    ) {
        return ResponseEntity.ok(service.adminUpsert(userId, dto));
    }
}

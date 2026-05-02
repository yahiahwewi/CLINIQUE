package com.example.demo.controller;

import com.example.demo.dto.LabOrderDTO;
import com.example.demo.dto.LabResultRequest;
import com.example.demo.service.LabService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lab-orders")
public class LabController {

    private final LabService service;

    public LabController(LabService service) {
        this.service = service;
    }

    @GetMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<List<LabOrderDTO>> byAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(service.findByAppointment(appointmentId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<LabOrderDTO>> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.findByPatient(patientId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<LabOrderDTO> create(@Valid @RequestBody LabOrderDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<LabOrderDTO> startProgress(@PathVariable Long id) {
        return ResponseEntity.ok(service.startProgress(id));
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<LabOrderDTO> uploadResult(@PathVariable Long id, @Valid @RequestBody LabResultRequest req) {
        return ResponseEntity.ok(service.uploadResult(id, req));
    }
}

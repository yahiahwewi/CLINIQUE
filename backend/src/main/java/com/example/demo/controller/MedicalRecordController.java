package com.example.demo.controller;

import com.example.demo.dto.MedicalRecordDTO;
import com.example.demo.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService service;

    public MedicalRecordController(MedicalRecordService service) {
        this.service = service;
    }

    @GetMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<MedicalRecordDTO> getByAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(service.getByAppointmentId(appointmentId));
    }

    @PutMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<MedicalRecordDTO> upsert(
            @PathVariable Long appointmentId,
            @Valid @RequestBody MedicalRecordDTO dto
    ) {
        return ResponseEntity.ok(service.upsert(appointmentId, dto));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecordDTO>> getForPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.getForPatient(patientId));
    }
}

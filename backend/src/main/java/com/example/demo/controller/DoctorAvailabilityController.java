package com.example.demo.controller;

import com.example.demo.dto.DoctorAvailabilityDTO;
import com.example.demo.dto.DoctorAvailabilityRequest;
import com.example.demo.dto.TimeSlotDTO;
import com.example.demo.service.DoctorAvailabilityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/availability")
public class DoctorAvailabilityController {

    private final DoctorAvailabilityService availabilityService;

    public DoctorAvailabilityController(DoctorAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<DoctorAvailabilityDTO>> getMyAvailability() {
        return ResponseEntity.ok(availabilityService.getMyAvailability());
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorAvailabilityDTO> addMyAvailability(@Valid @RequestBody DoctorAvailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.addMyAvailability(request));
    }

    @PutMapping("/me/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorAvailabilityDTO> updateMyAvailability(
            @PathVariable Long id,
            @Valid @RequestBody DoctorAvailabilityRequest request
    ) {
        return ResponseEntity.ok(availabilityService.updateMyAvailability(id, request));
    }

    @DeleteMapping("/me/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> deleteMyAvailability(@PathVariable Long id) {
        availabilityService.deleteMyAvailability(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/doctors/{doctorId}/slots")
    public ResponseEntity<List<TimeSlotDTO>> getSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(availabilityService.getSlotsForDoctorOnDate(doctorId, date));
    }

    @GetMapping("/doctors/{doctorId}")
    public ResponseEntity<List<DoctorAvailabilityDTO>> getDoctorAvailability(@PathVariable Long doctorId) {
        return ResponseEntity.ok(availabilityService.getAvailabilityForDoctor(doctorId));
    }
}

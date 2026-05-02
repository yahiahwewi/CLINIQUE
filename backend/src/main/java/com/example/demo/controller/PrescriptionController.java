package com.example.demo.controller;

import com.example.demo.dto.PrescriptionDTO;
import com.example.demo.entity.Prescription;
import com.example.demo.service.PrescriptionPdfService;
import com.example.demo.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prescriptions")
public class PrescriptionController {

    private final PrescriptionService service;
    private final PrescriptionPdfService pdfService;

    public PrescriptionController(PrescriptionService service, PrescriptionPdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @GetMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<List<PrescriptionDTO>> byAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(service.findByAppointment(appointmentId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PrescriptionDTO>> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.findByPatient(patientId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<PrescriptionDTO> create(@Valid @RequestBody PrescriptionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/{id}/dispense")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<PrescriptionDTO> dispense(@PathVariable Long id) {
        return ResponseEntity.ok(service.markDispensed(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Prescription rx = service.getEntityForPdf(id);
        byte[] pdf = pdfService.render(rx);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"prescription-" + id + ".pdf\"")
                .body(pdf);
    }
}

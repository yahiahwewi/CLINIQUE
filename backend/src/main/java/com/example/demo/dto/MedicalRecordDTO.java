package com.example.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordDTO {
    private Long id;
    private Long appointmentId;
    private String patientName;
    private String doctorName;
    private LocalDateTime appointmentDateTime;

    @Size(max = 1000) private String chiefComplaint;
    @Size(max = 2000) private String diagnosis;
    @Size(max = 3000) private String plan;
    @Size(max = 3000) private String privateNotes;
    @Size(max = 2000) private String patientSummary;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

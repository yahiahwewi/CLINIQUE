package com.example.demo.dto;

import com.example.demo.entity.LabOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class LabOrderDTO {
    private Long id;

    @NotNull
    private Long appointmentId;

    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;

    @NotBlank
    @Size(max = 200)
    private String testName;

    @Size(max = 1000)
    private String instructions;

    private LabOrder.Status status;

    /** Read-only — set when the lab tech uploads a result. */
    private String resultText;
    private Boolean abnormal;
    private String completedByName;
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
}

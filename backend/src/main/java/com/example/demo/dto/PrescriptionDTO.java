package com.example.demo.dto;

import com.example.demo.entity.Prescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Long id;
        @NotBlank @Size(max = 200) private String drugName;
        @Size(max = 100) private String dose;
        @Size(max = 100) private String frequency;
        private Integer durationDays;
        @Size(max = 500) private String notes;
    }

    private Long id;

    @NotNull
    private Long appointmentId;

    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;

    @Size(max = 1000)
    private String instructions;

    private Prescription.Status status;

    private String dispensedByName;
    private LocalDateTime dispensedAt;

    @Valid
    @Builder.Default
    private List<Item> items = new ArrayList<>();

    private LocalDateTime createdAt;
}

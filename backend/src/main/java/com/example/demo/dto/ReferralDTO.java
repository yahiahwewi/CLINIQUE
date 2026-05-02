package com.example.demo.dto;

import com.example.demo.entity.Referral;
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
public class ReferralDTO {
    private Long id;

    private Long fromDoctorId;
    private String fromDoctorName;

    @NotNull
    private Long toDoctorId;
    private String toDoctorName;

    @NotNull
    private Long patientId;
    private String patientName;

    private Long appointmentId;

    @NotBlank
    @Size(max = 1500)
    private String reason;

    private Referral.Status status;
    private LocalDateTime createdAt;
}

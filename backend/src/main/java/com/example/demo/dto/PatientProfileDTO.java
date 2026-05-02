package com.example.demo.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProfileDTO {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;

    @Past
    private LocalDate dateOfBirth;

    @Size(max = 24)
    private String gender;

    @Size(max = 8)
    private String bloodType;

    @Size(max = 1000)
    private String allergies;

    @Size(max = 1000)
    private String chronicConditions;

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 40)
    private String emergencyContactPhone;
}

package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorProfileDTO {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;

    /** IDs of departments this doctor belongs to (for write/update). */
    private Set<Long> departmentIds;

    /** Read-only — populated by the server for display. */
    private List<DepartmentDTO> departments;

    @Size(max = 80)
    private String specialty;

    @Size(max = 60)
    private String licenseNumber;

    @Size(max = 2000)
    private String bio;

    @Size(max = 200)
    private String languages;

    @Min(0)
    private Integer consultationFeeCents;

    @Min(0)
    private Integer yearsExperience;

    @Size(max = 500)
    private String photoUrl;
}

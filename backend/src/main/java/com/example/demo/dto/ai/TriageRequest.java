package com.example.demo.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageRequest {
    @NotBlank
    @Size(max = 1500)
    private String symptoms;

    private Integer ageYears;
    @Size(max = 24)
    private String gender;
}

package com.example.demo.dto;

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
public class DepartmentDTO {
    private Long id;

    @NotBlank
    @Size(max = 80)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 16)
    private String color;

    @Size(max = 32)
    private String icon;
}

package com.example.demo.dto;

import com.example.demo.entity.TimeOff;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeOffDTO {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private String doctorEmail;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @Size(max = 500)
    private String reason;

    private TimeOff.Status status;

    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private String decidedByName;

    @Size(max = 300)
    private String decisionNote;
}

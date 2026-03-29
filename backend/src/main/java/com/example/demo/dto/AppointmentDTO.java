package com.example.demo.dto;

import com.example.demo.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime appointmentDateTime;
    private AppointmentStatus status;
    private UserOptionDTO patient;
    private UserOptionDTO doctor;
    private UserOptionDTO nurse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

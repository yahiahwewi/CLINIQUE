package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatisticsDTO {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long totalAppointments;
    private long pendingAppointments;
    private long acceptedAppointments;
}


package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    private Long id;
    private String actorName;
    private String actorEmail;
    private String action;
    private String entityType;
    private Long entityId;
    private String summary;
    private LocalDateTime createdAt;
}

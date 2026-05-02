package com.example.demo.dto;

import com.example.demo.entity.Announcement;
import jakarta.validation.constraints.NotBlank;
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
public class AnnouncementDTO {
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 1500)
    private String body;

    private Announcement.Audience audience;
    private Announcement.Tone tone;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}

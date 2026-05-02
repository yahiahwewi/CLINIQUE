package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private java.util.List<String> roles;

    /** APPROVED / PENDING / REJECTED. When PENDING, token is empty and the
     *  client should not save credentials — show a "pending approval" page. */
    private String approvalStatus;

    /** Human-friendly message — used by the registration flow to explain
     *  pending state without forcing the client to translate enum values. */
    private String message;
}


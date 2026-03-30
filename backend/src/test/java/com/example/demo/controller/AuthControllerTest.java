package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void loginReturnsJwtPayload() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("token")
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .roles(List.of("ROLE_ADMIN"))
                .build();

        when(authService.login(new LoginRequest("admin@example.com", "admin123"))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@example.com", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void forgotPasswordReturnsGenericMessage() throws Exception {
        doNothing().when(authService).requestPasswordReset(new ForgotPasswordRequest("john@example.com"));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("john@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the account exists, a reset link has been generated."));
    }

    @Test
    void resetPasswordReturnsSuccessMessage() throws Exception {
        doNothing().when(authService).resetPassword(new ResetPasswordRequest("token", "newpassword", "newpassword"));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("token", "newpassword", "newpassword"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful. You can now sign in."));
    }
}

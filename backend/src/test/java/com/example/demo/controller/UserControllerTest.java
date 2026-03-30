package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void currentUserEndpointReturnsProfile() throws Exception {
        UserDTO user = UserDTO.builder()
                .id(1L)
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .enabled(true)
                .build();

        when(userService.getUserByEmail("admin@example.com")).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin@example.com", null));

        mockMvc.perform(get("/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));

        SecurityContextHolder.clearContext();
    }

    @Test
    void adminUpdateReturnsUpdatedUser() throws Exception {
        UserDTO request = UserDTO.builder()
                .id(2L)
                .email("doctor@example.com")
                .firstName("Emily")
                .lastName("Stone")
                .enabled(true)
                .build();

        when(userService.updateUser(2L, request)).thenReturn(request);

        mockMvc.perform(put("/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("doctor@example.com"));
    }
}

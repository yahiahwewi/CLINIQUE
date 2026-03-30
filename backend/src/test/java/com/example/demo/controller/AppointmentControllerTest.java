package com.example.demo.controller;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.AppointmentStatusUpdateRequest;
import com.example.demo.dto.UserOptionDTO;
import com.example.demo.entity.AppointmentStatus;
import com.example.demo.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AppointmentController.class, properties = "server.servlet.context-path=")
@AutoConfigureMockMvc(addFilters = false)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    void createAppointmentReturnsCreatedAppointment() throws Exception {
        AppointmentRequest request = AppointmentRequest.builder()
                .title("Checkup")
                .description("Routine visit")
                .appointmentDateTime(LocalDateTime.of(2030, 1, 10, 9, 0))
                .doctorId(2L)
                .nurseId(3L)
                .build();

        AppointmentDTO response = AppointmentDTO.builder()
                .id(1L)
                .title("Checkup")
                .description("Routine visit")
                .status(AppointmentStatus.PENDING)
                .doctor(UserOptionDTO.builder().id(2L).fullName("Emily Stone").email("doctor@example.com").build())
                .patient(UserOptionDTO.builder().id(4L).fullName("John Doe").email("john@example.com").build())
                .appointmentDateTime(request.getAppointmentDateTime())
                .build();

        when(appointmentService.createAppointment(request)).thenReturn(response);

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Checkup"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void statusUpdateReturnsUpdatedAppointment() throws Exception {
        AppointmentDTO response = AppointmentDTO.builder()
                .id(1L)
                .title("Checkup")
                .status(AppointmentStatus.ACCEPTED)
                .build();

        when(appointmentService.updateStatus(1L, AppointmentStatus.ACCEPTED)).thenReturn(response);

        mockMvc.perform(patch("/appointments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentStatusUpdateRequest(AppointmentStatus.ACCEPTED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }
}

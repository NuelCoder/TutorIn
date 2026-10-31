package com.TutorIn.controller;

import com.TutorIn.dto.PendingTutorDTO;
import com.TutorIn.dto.UpdateTutor;
import com.TutorIn.entity.PendingTutor;
import com.TutorIn.entity.Tutor;
import com.TutorIn.enums.Role;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import com.TutorIn.service.PENDINGTUTOR.PendingTutorService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import com.TutorIn.service.TUTOR.TutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TutorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TutorService tutorService;

    @MockitoBean
    private PendingTutorService pendingTutorService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    // Kept mocked to prevent the ApplicationContext from crashing
    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    public void whenRegisterTutor_thenReturn200() throws Exception {
        // This endpoint should be publicly accessible
        PendingTutorDTO request = new PendingTutorDTO(
                "Jane Doe", "Expert in Math", "jane@tutorin.com", "Female", "StrongPass123!", "Nigeria"
        );

        PendingTutor mockPendingTutor = new PendingTutor();
        mockPendingTutor.setPendingTutorId(UUID.randomUUID());
        mockPendingTutor.setEmail("jane@tutorin.com");
        mockPendingTutor.setRole(Role.PENDING);
        mockPendingTutor.setCreatedAt(OffsetDateTime.now());

        when(pendingTutorService.registerTutor(any(PendingTutorDTO.class))).thenReturn(mockPendingTutor);

        mockMvc.perform(post("/api/v1/tutors/register-tutor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile has been filled"))
                .andExpect(jsonPath("$.pending.email").value("jane@tutorin.com"));
    }

    @Test
    @WithMockUser(username = "jane@tutorin.com", authorities = "ROLE_TUTOR") // Injecting the identity
    public void whenGetTutorProfile_thenReturn200() throws Exception {
        Tutor mockTutor = new Tutor();
        mockTutor.setTutorId(UUID.randomUUID());
        mockTutor.setName("Jane Doe");
        mockTutor.setEmail("jane@tutorin.com");
        mockTutor.setRole(Role.TUTOR);

        when(tutorService.getTutorProfile(anyString())).thenReturn(mockTutor);

        mockMvc.perform(get("/api/v1/tutors/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@tutorin.com"));
    }

    @Test
    @WithMockUser(username = "jane@tutorin.com", authorities = "ROLE_TUTOR")
    public void whenUpdateTutorWithInvalidEmail_thenReturn400() throws Exception {
        // Passing an invalid email to trigger the @Valid failure on UpdateTutor DTO
        UpdateTutor request = new UpdateTutor("Jane", "Female", "Bio here", "invalid-email-format", "Nigeria");

        mockMvc.perform(put("/api/v1/tutors/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Validates 400 failure case
    }

    @Test
    public void whenAccessingProfileWithoutAuth_thenReturn403() throws Exception {
        // No @WithMockUser provided; Spring Security should block this
        mockMvc.perform(get("/api/v1/tutors/me"))
                .andExpect(status().isForbidden()); // Validates 403 failure case
    }
}
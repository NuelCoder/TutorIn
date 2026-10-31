package com.TutorIn.controller;

import com.TutorIn.dto.TutorSearchResponse;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import com.TutorIn.service.TUTORSUBJECT.TutorSubjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TutorSubjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TutorSubjectService tutorSubjectService;

    // Kept mocked to prevent the ApplicationContext from crashing
    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    @WithMockUser(username = "tutor@tutorin.com", authorities = "ROLE_TUTOR")
    public void whenAssignSubject_thenReturn200() throws Exception {
        String jsonRequest = """
                {
                    "subjectId": "123e4567-e89b-12d3-a456-426614174000",
                    "hourlyRate": 25.50
                }
                """;

        // Mocking a void method
        doNothing().when(tutorSubjectService).assignSubjectToTutor(anyString(), any());

        mockMvc.perform(post("/api/v1/tutor-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully registered to teach this subject."));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    public void whenSearchTutors_thenReturn200() throws Exception {
        TutorSearchResponse mockResponse = new TutorSearchResponse(
                UUID.randomUUID(), "John Tutor", "john@tutorin.com", "Expert in Math", "Physics", new BigDecimal("25.000")
        );

        when(tutorSubjectService.searchTutorsBySubject("Math")).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/v1/tutor-subjects/search")
                        .param("title", "Math"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tutorName").value("John Tutor"));
    }

    @Test
    public void whenSearchTutorsWithoutAuth_thenReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/tutor-subjects/search")
                        .param("title", "Math"))
                .andExpect(status().isForbidden());
    }
}
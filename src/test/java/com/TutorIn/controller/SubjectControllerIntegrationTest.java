package com.TutorIn.controller;

import com.TutorIn.dto.SubjectRequest;
import com.TutorIn.entity.Subject;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import com.TutorIn.service.SUBJECT.SubjectService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SubjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubjectService subjectService;

    // Kept mocked to prevent the ApplicationContext from crashing
    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    @WithMockUser(roles = "ADMIN") // Simulates an authenticated admin user
    public void whenCreateSubjectWithValidData_thenReturn201() throws Exception {
        SubjectRequest request = new SubjectRequest("Physics", "Advanced Physics");
        Subject mockSubject = new Subject();
        mockSubject.setSubjectTitle("Physics");
        mockSubject.setDescription("Advanced Physics");

        // Mock the service response
        when(subjectService.createSubject(any(SubjectRequest.class))).thenReturn(mockSubject);

        mockMvc.perform(post("/api/v1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectTitle").value("Physics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void whenCreateSubjectWithMissingTitle_thenReturn400() throws Exception {
        // Missing subjectTitle to trigger the @Valid failure on your Controller
        SubjectRequest request = new SubjectRequest("", "Advanced Physics");

        mockMvc.perform(post("/api/v1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void whenGetAllSubjects_thenReturn200() throws Exception {
        Subject mockSubject = new Subject();
        mockSubject.setSubjectTitle("Math");

        when(subjectService.getAllSubjects()).thenReturn(List.of(mockSubject));

        mockMvc.perform(get("/api/v1/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectTitle").value("Math"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void whenDeleteSubject_thenReturn200() throws Exception {
        UUID subjectId = UUID.randomUUID();

        // doNothing() is used for void methods in Mockito
        doNothing().when(subjectService).deleteSubject(subjectId);

        mockMvc.perform(delete("/api/v1/subjects/" + subjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subject successfully deleted."));
    }

    @Test
    public void whenAccessingSubjectsWithoutAuth_thenReturn403() throws Exception {
        // No @WithMockUser provided; Spring Security should intercept and block this
        mockMvc.perform(get("/api/v1/subjects"))
                .andExpect(status().isForbidden());
    }
}
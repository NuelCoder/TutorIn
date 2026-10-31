package com.TutorIn.controller;

import com.TutorIn.dto.AdminResolveReportRequest;
import com.TutorIn.dto.ReportRequest;
import com.TutorIn.dto.ReportResponse;
import com.TutorIn.enums.RoleType;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import com.TutorIn.service.REPORT.ReportService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReportService reportService;

    // Kept mocked to prevent the ApplicationContext from crashing
    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    @WithMockUser(authorities = "ROLE_USER") // Simulating an authenticated student reporting a tutor
    public void whenCreateReport_thenReturn201() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        ReportRequest request = new ReportRequest("bad.tutor@tutorin.com", RoleType.TUTOR, sessionId, "Inappropriate language");

        ReportResponse mockResponse = new ReportResponse(
                reportId, "student@tutorin.com", "bad.tutor@tutorin.com", sessionId,
                "Inappropriate language", null, "PENDING", OffsetDateTime.now()
        );

        // Mock the service call
        when(reportService.createReport(anyString(), anyBoolean(), any(ReportRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(reportId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    public void whenCreateReportWithInvalidEmail_thenReturn400() throws Exception {
        // "not-an-email" will trigger your @Email validation constraint in ReportRequest
        ReportRequest request = new ReportRequest("not-an-email", RoleType.TUTOR, UUID.randomUUID(), "Reason");

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Validates the 400 Bad Request requirement
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN") // Simulating an admin retrieving all reports
    public void whenGetAllReports_thenReturn200() throws Exception {
        ReportResponse mockResponse = new ReportResponse(
                UUID.randomUUID(), "student@tutorin.com", "bad.tutor@tutorin.com", UUID.randomUUID(),
                "Reason", null, "PENDING", OffsetDateTime.now()
        );

        when(reportService.getAllReports()).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportedEmail").value("bad.tutor@tutorin.com"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN") // Simulating an admin resolving a report
    public void whenResolveReport_thenReturn200() throws Exception {
        UUID reportId = UUID.randomUUID();
        AdminResolveReportRequest request = new AdminResolveReportRequest("Warned the tutor", "RESOLVED");

        ReportResponse mockResponse = new ReportResponse(
                reportId, "student@tutorin.com", "bad.tutor@tutorin.com", UUID.randomUUID(),
                "Reason", "Warned the tutor", "RESOLVED", OffsetDateTime.now()
        );

        when(reportService.resolveReport(eq(reportId), any(AdminResolveReportRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(put("/api/v1/reports/" + reportId + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminResolutionNotes").value("Warned the tutor"));
    }

    @Test
    public void whenAccessingReportsWithoutAuth_thenReturn403() throws Exception {
        // No @WithMockUser provided; Spring Security should forbid access
        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isForbidden()); // Validates the 403 failure case requirement
    }
}
package com.TutorIn.controller;

import com.TutorIn.dto.BookSessionRequest;
import com.TutorIn.dto.SessionResponse;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import com.TutorIn.service.SESSION.SessionService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SessionService sessionService;

    // Kept mocked to prevent the ApplicationContext from crashing
    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    @WithMockUser(authorities = "ROLE_USER") // Simulating an authenticated student
    public void whenBookSession_thenReturn201() throws Exception {
        UUID slotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        BookSessionRequest request = new BookSessionRequest(slotId);

        SessionResponse mockResponse = new SessionResponse(
                sessionId, slotId, "John Tutor", "Alice Student",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), "BOOKED"
        );

        when(sessionService.bookSession(any(BookSessionRequest.class), any(String.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.studentName").value("Alice Student"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    public void whenBookSessionWithoutSlotId_thenReturn400() throws Exception {
        // Passing null triggers the @NotNull validation failure in BookSessionRequest
        BookSessionRequest request = new BookSessionRequest(null);

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Validates validation bounds
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    public void whenGetStudentSessions_thenReturn200() throws Exception {
        SessionResponse mockResponse = new SessionResponse(
                UUID.randomUUID(), UUID.randomUUID(), "John Tutor", "Alice Student",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), "BOOKED"
        );

        when(sessionService.getUpcomingStudentSessions(any(String.class))).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/v1/sessions/student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tutorName").value("John Tutor"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_TUTOR") // Simulating a tutor canceling a session
    public void whenCancelSession_thenReturn200() throws Exception {
        UUID sessionId = UUID.randomUUID();

        // doNothing() used for void methods in Mockito
        doNothing().when(sessionService).cancelSession(any(UUID.class), any(String.class), any(Boolean.class));

        mockMvc.perform(put("/api/v1/sessions/" + sessionId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Session successfully cancelled."));
    }

    @Test
    public void whenAccessingSessionsWithoutAuth_thenReturn403() throws Exception {
        // No @WithMockUser provided; Spring Security should forbid access
        mockMvc.perform(get("/api/v1/sessions/student"))
                .andExpect(status().isForbidden());
    }
}
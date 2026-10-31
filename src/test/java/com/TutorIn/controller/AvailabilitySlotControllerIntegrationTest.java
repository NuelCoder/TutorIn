package com.TutorIn.controller;

import com.TutorIn.dto.SlotCreateRequest;
import com.TutorIn.dto.SlotResponse;
import com.TutorIn.service.AVAILABILITYSLOT.AvailabilitySlotService;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AvailabilitySlotControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AvailabilitySlotService slotService;

    // Kept mocked to prevent the ApplicationContext from crashing
    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    @WithMockUser(authorities = "ROLE_TUTOR") // Simulating an authenticated tutor
    public void whenCreateSlot_thenReturns201() throws Exception {
        UUID tutorId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        // Creating a valid future date
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endTime = startTime.plusHours(1);

        SlotCreateRequest request = new SlotCreateRequest(tutorId, startTime, endTime);
        SlotResponse mockResponse = new SlotResponse(slotId, tutorId, startTime, endTime, false);

        // Tell the mock service what to return
        when(slotService.createSlot(any(SlotCreateRequest.class), any(String.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(slotId.toString()));
    }

    @Test
    @WithMockUser(authorities = "ROLE_TUTOR")
    public void whenCreateSlotInPast_thenReturns400() throws Exception {
        UUID tutorId = UUID.randomUUID();

        // Start time in the past should trigger your @Future validation failure!
        OffsetDateTime startTime = OffsetDateTime.now().minusDays(1);
        OffsetDateTime endTime = startTime.plusHours(1);

        SlotCreateRequest request = new SlotCreateRequest(tutorId, startTime, endTime);

        mockMvc.perform(post("/api/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Proves validation blocks bad HTTP requests
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER") // Users should be able to retrieve a tutor's slots
    public void whenGetTutorSlots_thenReturns200() throws Exception {
        UUID tutorId = UUID.randomUUID();
        SlotResponse mockResponse = new SlotResponse(
                UUID.randomUUID(), tutorId, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1), false
        );

        when(slotService.getTutorSlots(tutorId)).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/v1/slots/tutor/" + tutorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tutorId").value(tutorId.toString()));
    }

    @Test
    public void whenAccessingSlotsWithoutAuth_thenReturns403() throws Exception {
        // No @WithMockUser provided; Spring Security should forbid access
        mockMvc.perform(get("/api/v1/slots/tutor/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
package com.TutorIn.service.AVAILABILITYSLOT;

import com.TutorIn.dto.SlotCreateRequest;
import com.TutorIn.dto.SlotResponse;
import com.TutorIn.dto.SlotUpdateRequest;
import com.TutorIn.entity.AvailabilitySlot;
import com.TutorIn.entity.Tutor;
import com.TutorIn.exceptions.AccessDeniedException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AvailabilitySlotRepository;
import com.TutorIn.repository.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AvailabilitySlotServiceTest {

    @Mock
    private AvailabilitySlotRepository slotRepository;

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private AvailabilitySlotService slotService;

    private Tutor mockTutor;
    private AvailabilitySlot mockSlot;
    private UUID tutorId;
    private UUID slotId;
    private String tutorEmail;

    @BeforeEach
    public void setUp() {
        tutorId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        tutorEmail = "tutor@tutorin.com";

        mockTutor = new Tutor();
        mockTutor.setTutorId(tutorId);
        mockTutor.setEmail(tutorEmail);

        mockSlot = new AvailabilitySlot();
        mockSlot.setSlotId(slotId);
        mockSlot.setTutor(mockTutor);
        mockSlot.setStartTime(OffsetDateTime.now().plusDays(1));
        mockSlot.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        mockSlot.setBooked(false);
    }

    @Test
    public void createSlot_whenValid_thenReturnSlotResponse() {
        SlotCreateRequest request = new SlotCreateRequest(tutorId, mockSlot.getStartTime(), mockSlot.getEndTime());

        when(tutorRepository.findByEmail(tutorEmail)).thenReturn(Optional.of(mockTutor));
        when(slotRepository.countOverlappingSlots(tutorId, request.startTime(), request.endTime())).thenReturn(0L);
        when(slotRepository.save(any(AvailabilitySlot.class))).thenReturn(mockSlot);

        SlotResponse response = slotService.createSlot(request, tutorEmail);

        assertNotNull(response);
        assertEquals(slotId, response.id());
        verify(slotRepository, times(1)).save(any(AvailabilitySlot.class));
    }

    @Test
    public void createSlot_whenTimeOverlaps_thenThrowsBadRequest() {
        SlotCreateRequest request = new SlotCreateRequest(tutorId, mockSlot.getStartTime(), mockSlot.getEndTime());

        when(tutorRepository.findByEmail(tutorEmail)).thenReturn(Optional.of(mockTutor));
        when(slotRepository.countOverlappingSlots(tutorId, request.startTime(), request.endTime())).thenReturn(1L); // 1 overlap found

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                slotService.createSlot(request, tutorEmail));

        assertEquals("Time slot overlaps with existing schedule", exception.getMessage());
        verify(slotRepository, never()).save(any(AvailabilitySlot.class));
    }

    @Test
    public void createSlot_whenEndTimeBeforeStartTime_thenThrowsBadRequest() {
        // End time is before start time
        SlotCreateRequest request = new SlotCreateRequest(tutorId, mockSlot.getEndTime(), mockSlot.getStartTime());

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                slotService.createSlot(request, tutorEmail));

        assertEquals("End time must be after start time", exception.getMessage());
    }

    @Test
    public void updateSlot_whenNotSlotOwner_thenThrowsAccessDenied() {
        SlotUpdateRequest request = new SlotUpdateRequest(mockSlot.getStartTime(), mockSlot.getEndTime());
        String wrongEmail = "hacker@tutorin.com";

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(mockSlot));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                slotService.updateSlot(slotId, request, wrongEmail));

        assertEquals("You do not have permission to modify this slot", exception.getMessage());
    }

    @Test
    public void deleteSlot_whenAlreadyBooked_thenThrowsBadRequest() {
        mockSlot.setBooked(true); // Mark as booked

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(mockSlot));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                slotService.deleteSlot(slotId, tutorEmail));

        assertEquals("Cannot delete a booked slot. Cancel the session first.", exception.getMessage());
        verify(slotRepository, never()).delete(any(AvailabilitySlot.class));
    }

    @Test
    public void getTutorSlots_whenValidTutorId_thenReturnList() {
        when(tutorRepository.existsById(tutorId)).thenReturn(true);
        when(slotRepository.findByTutor_TutorIdOrderByStartTimeAsc(tutorId)).thenReturn(List.of(mockSlot));

        List<SlotResponse> responses = slotService.getTutorSlots(tutorId);

        assertEquals(1, responses.size());
        assertEquals(slotId, responses.get(0).id());
    }
}
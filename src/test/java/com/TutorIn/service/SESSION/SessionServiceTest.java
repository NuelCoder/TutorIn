package com.TutorIn.service.SESSION;

import com.TutorIn.dto.BookSessionRequest;
import com.TutorIn.dto.SessionResponse;
import com.TutorIn.entity.AvailabilitySlot;
import com.TutorIn.entity.Session;
import com.TutorIn.entity.Tutor;
import com.TutorIn.entity.User;
import com.TutorIn.enums.SessionStatus;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AvailabilitySlotRepository;
import com.TutorIn.repository.SessionRepository;
import com.TutorIn.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private AvailabilitySlotRepository slotRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SessionService sessionService;

    @Test
    public void bookSession_whenValid_thenMarksSlotBookedAndSavesSession() {
        String studentEmail = "student@tutorin.com";
        UUID slotId = UUID.randomUUID();
        BookSessionRequest request = new BookSessionRequest(slotId);

        User mockStudent = new User();
        mockStudent.setName("Alice");

        AvailabilitySlot mockSlot = new AvailabilitySlot();
        mockSlot.setBooked(false);
        Tutor mockTutor = new Tutor();
        mockTutor.setName("Bob");
        mockSlot.setTutor(mockTutor);

        when(userRepository.findByEmail(studentEmail)).thenReturn(Optional.of(mockStudent));
        when(slotRepository.findByIdWithPessimisticLock(slotId)).thenReturn(Optional.of(mockSlot));

        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> {
            Session s = i.getArgument(0);
            s.setSessionId(UUID.randomUUID());
            return s;
        });

        SessionResponse response = sessionService.bookSession(request, studentEmail);

        assertNotNull(response);
        assertTrue(mockSlot.isBooked());
        verify(slotRepository, times(1)).save(mockSlot);
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    public void bookSession_whenSlotAlreadyBooked_thenThrowsBadRequest() {
        String studentEmail = "student@tutorin.com";
        UUID slotId = UUID.randomUUID();
        BookSessionRequest request = new BookSessionRequest(slotId);

        AvailabilitySlot mockSlot = new AvailabilitySlot();
        mockSlot.setBooked(true); // Already booked

        when(userRepository.findByEmail(studentEmail)).thenReturn(Optional.of(new User()));
        when(slotRepository.findByIdWithPessimisticLock(slotId)).thenReturn(Optional.of(mockSlot));

        assertThrows(BadRequestException.class, () -> sessionService.bookSession(request, studentEmail));
        verify(sessionRepository, never()).save(any());
    }
}
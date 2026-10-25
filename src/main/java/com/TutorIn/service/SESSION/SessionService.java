package com.TutorIn.service.SESSION;

import com.TutorIn.dto.BookSessionRequest;
import com.TutorIn.dto.SessionResponse;
import com.TutorIn.entity.AvailabilitySlot;
import com.TutorIn.entity.Session;
import com.TutorIn.entity.User;
import com.TutorIn.enums.SessionStatus;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AvailabilitySlotRepository;
import com.TutorIn.repository.SessionRepository;
import com.TutorIn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public SessionResponse bookSession(BookSessionRequest request, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new BadRequestException("Student not found."));

        AvailabilitySlot slot = slotRepository.findByIdWithPessimisticLock(request.slotId())
                .orElseThrow(() -> new BadRequestException("Slot not found."));

        if (slot.isBooked()) {
            throw new BadRequestException("This slot has already been booked by another student.");
        }

        slot.setBooked(true);
        slotRepository.save(slot);

        Session session = new Session();
        session.setUser(student);
        session.setSlot(slot);
        session.setStatus(SessionStatus.BOOKED);

        Session savedSession = sessionRepository.save(session);

        return mapToResponse(savedSession);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'TUTOR')")
    public void cancelSession(UUID sessionId, String userEmail, boolean isTutor) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BadRequestException("Session not found."));

        if (isTutor) {
            if (!session.getSlot().getTutor().getEmail().equals(userEmail)) {
                throw new BadRequestException("You do not have permission to cancel this session.");
            }
        } else {
            if (!session.getUser().getEmail().equals(userEmail)) {
                throw new BadRequestException("You do not have permission to cancel this session.");
            }
        }

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BadRequestException("Session is already cancelled.");
        }

        session.getSlot().setBooked(false);
        session.setStatus(SessionStatus.CANCELLED);

        slotRepository.save(session.getSlot());
        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<SessionResponse> getUpcomingStudentSessions(String studentEmail) {
        return sessionRepository.findUpcomingSessionsForStudent(studentEmail)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('TUTOR')")
    public List<SessionResponse> getUpcomingTutorSessions(String tutorEmail) {
        return sessionRepository.findUpcomingSessionsForTutor(tutorEmail)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SessionResponse mapToResponse(Session session) {
        return new SessionResponse(
                session.getSessionId(),
                session.getSlot().getSlotId(),
                session.getSlot().getTutor().getName(),
                session.getUser().getName(),
                session.getSlot().getStartTime(),
                session.getSlot().getEndTime(),
                session.getStatus().name()
        );
    }
}
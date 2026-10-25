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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilitySlotService {

    private final AvailabilitySlotRepository slotRepository;
    private final TutorRepository tutorRepository;

    @Transactional
    @PreAuthorize("hasRole('TUTOR')")
    public SlotResponse createSlot(SlotCreateRequest request, String tutorEmail) {
        validateTimeLogic(request.startTime(), request.endTime());

        Tutor tutor = tutorRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new BadRequestException("Tutor not found"));

        if (slotRepository.countOverlappingSlots(tutor.getTutorId(), request.startTime(), request.endTime()) > 0) {
            throw new BadRequestException("Time slot overlaps with existing schedule");
        }

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setTutor(tutor);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());

        AvailabilitySlot savedSlot = slotRepository.save(slot);
        return mapToResponse(savedSlot);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<SlotResponse> getTutorSlots(UUID tutorId) {
        if (!tutorRepository.existsById(tutorId)) {
            throw new BadRequestException("Tutor not found");
        }
        return slotRepository.findByTutor_TutorIdOrderByStartTimeAsc(tutorId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public SlotResponse getSlot(UUID slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new BadRequestException("Slot not found"));
        return mapToResponse(slot);
    }

    @Transactional
    @PreAuthorize("hasRole('TUTOR')")
    public SlotResponse updateSlot(UUID slotId, SlotUpdateRequest request, String tutorEmail) {
        validateTimeLogic(request.startTime(), request.endTime());

        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new BadRequestException("Slot not found"));

        if (!slot.getTutor().getEmail().equals(tutorEmail)) {
            throw new AccessDeniedException("You do not have permission to modify this slot");
        }

        if (slot.isBooked()) {
            throw new BadRequestException("Cannot update a slot that is already booked");
        }

        if (slotRepository.countOverlappingSlotsExcludingSelf(
                slot.getTutor().getTutorId(), slotId, request.startTime(), request.endTime()) > 0) {
            throw new BadRequestException("Updated time slot overlaps with existing schedule");
        }

        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());

        return mapToResponse(slotRepository.save(slot));
    }

    @Transactional
    @PreAuthorize("hasRole('TUTOR')")
    public void deleteSlot(UUID slotId, String tutorEmail) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new BadRequestException("Slot not found"));

        if (!slot.getTutor().getEmail().equals(tutorEmail)) {
            throw new AccessDeniedException("You do not have permission to delete this slot");
        }

        if (slot.isBooked()) {
            throw new BadRequestException("Cannot delete a booked slot. Cancel the session first.");
        }

        slotRepository.delete(slot);
    }

    private void validateTimeLogic(java.time.OffsetDateTime start, java.time.OffsetDateTime end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private SlotResponse mapToResponse(AvailabilitySlot slot) {
        return new SlotResponse(
                slot.getSlotId(),
                slot.getTutor().getTutorId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.isBooked()
        );
    }
}
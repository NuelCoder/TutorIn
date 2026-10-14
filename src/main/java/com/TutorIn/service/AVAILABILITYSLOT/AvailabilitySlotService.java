package com.TutorIn.service.AVAILABILITYSLOT;

import com.TutorIn.dto.SlotCreateRequest;
import com.TutorIn.dto.SlotResponse;
import com.TutorIn.dto.SlotUpdateRequest;
import com.TutorIn.entity.AvailabilitySlot;
import com.TutorIn.entity.Tutor;
import com.TutorIn.repository.AvailabilitySlotRepository;
import com.TutorIn.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilitySlotService {
    private final AvailabilitySlotRepository slotRepository;
    private final TutorRepository tutorRepository;

    @Transactional
    public SlotResponse createSlot(SlotCreateRequest request) {
        validateTimeLogic(request.startTime(), request.endTime());

        Tutor tutor = tutorRepository.findById(request.tutorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor not found"));

        if (slotRepository.countOverlappingSlots(tutor.getTutorId(), request.startTime(), request.endTime()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time slot overlaps with existing schedule");
        }

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setTutor(tutor);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());

        AvailabilitySlot savedSlot = slotRepository.save(slot);
        return mapToResponse(savedSlot);
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> getTutorSlots(UUID tutorId) {
        if (!tutorRepository.existsById(tutorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor not found");
        }
        return slotRepository.findByTutor_TutorIdOrderByStartTimeAsc(tutorId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public SlotResponse getSlot(UUID slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        return mapToResponse(slot);
    }

    @Transactional
    public SlotResponse updateSlot(UUID slotId, SlotUpdateRequest request) {
        validateTimeLogic(request.startTime(), request.endTime());

        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));

        if (slot.isBooked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update a slot that is already booked");
        }

        if (slotRepository.countOverlappingSlotsExcludingSelf(
                slot.getTutor().getTutorId(), slotId, request.startTime(), request.endTime()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Updated time slot overlaps with existing schedule");
        }

        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());

        return mapToResponse(slotRepository.save(slot));
    }

    @Transactional
    public void deleteSlot(UUID slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));

        if (slot.isBooked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete a booked slot. Cancel the session first.");
        }

        slotRepository.delete(slot);
    }

    private void validateTimeLogic(java.time.OffsetDateTime start, java.time.OffsetDateTime end) {
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
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

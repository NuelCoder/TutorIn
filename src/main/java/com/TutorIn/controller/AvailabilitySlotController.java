package com.TutorIn.controller;

import com.TutorIn.dto.SlotCreateRequest;
import com.TutorIn.dto.SlotResponse;
import com.TutorIn.dto.SlotUpdateRequest;
import com.TutorIn.service.AVAILABILITYSLOT.AvailabilitySlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class AvailabilitySlotController {

    private final AvailabilitySlotService slotService;

    @PostMapping
    public ResponseEntity<SlotResponse> createSlot(@Valid @RequestBody SlotCreateRequest request) {
        SlotResponse response = slotService.createSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<SlotResponse>> getTutorSlots(@PathVariable UUID tutorId) {
        return ResponseEntity.ok(slotService.getTutorSlots(tutorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SlotResponse> getSlot(@PathVariable UUID id) {
        return ResponseEntity.ok(slotService.getSlot(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SlotResponse> updateSlot(
            @PathVariable UUID id,
            @Valid @RequestBody SlotUpdateRequest request) {
        return ResponseEntity.ok(slotService.updateSlot(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable UUID id) {
        slotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}
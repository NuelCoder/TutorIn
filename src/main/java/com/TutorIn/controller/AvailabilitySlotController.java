package com.TutorIn.controller;

import com.TutorIn.dto.SlotCreateRequest;
import com.TutorIn.dto.SlotResponse;
import com.TutorIn.dto.SlotUpdateRequest;
import com.TutorIn.service.AVAILABILITYSLOT.AvailabilitySlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class AvailabilitySlotController {

    private final AvailabilitySlotService slotService;

    @PostMapping
    public ResponseEntity<SlotResponse> createSlot(
            @Valid @RequestBody SlotCreateRequest request,
            Authentication authentication) {

        SlotResponse response = slotService.createSlot(request, authentication.getName());
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
            @Valid @RequestBody SlotUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(slotService.updateSlot(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable UUID id,
            Authentication authentication) {

        slotService.deleteSlot(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
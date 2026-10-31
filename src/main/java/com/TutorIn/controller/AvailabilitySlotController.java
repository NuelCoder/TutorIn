package com.TutorIn.controller;

import com.TutorIn.dto.SlotCreateRequest;
import com.TutorIn.dto.SlotResponse;
import com.TutorIn.dto.SlotUpdateRequest;
import com.TutorIn.service.AVAILABILITYSLOT.AvailabilitySlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Availability Slots", description = "Endpoints for tutors to manage their availability schedule and students to view them")
@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class AvailabilitySlotController {

    private final AvailabilitySlotService slotService;

    @Operation(summary = "Create Availability Slot", description = "Allows an authenticated tutor to open a new time slot for booking.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Slot successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid time range or overlapping slot"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires TUTOR role")
    })
    @PostMapping
    public ResponseEntity<SlotResponse> createSlot(
            @Valid @RequestBody SlotCreateRequest request,
            Authentication authentication) {

        SlotResponse response = slotService.createSlot(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get Tutor's Slots", description = "Retrieves all active availability slots for a specific tutor.")
    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<SlotResponse>> getTutorSlots(@PathVariable UUID tutorId) {
        return ResponseEntity.ok(slotService.getTutorSlots(tutorId));
    }

    @Operation(summary = "Get Slot Details", description = "Retrieves details of a specific availability slot.")
    @GetMapping("/{id}")
    public ResponseEntity<SlotResponse> getSlot(@PathVariable UUID id) {
        return ResponseEntity.ok(slotService.getSlot(id));
    }

    @Operation(summary = "Update Availability Slot", description = "Allows a tutor to update the time bounds of an existing, unbooked slot.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Slot successfully updated"),
            @ApiResponse(responseCode = "400", description = "Slot is already booked or invalid time range"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the slot owner")
    })
    @PutMapping("/{id}")
    public ResponseEntity<SlotResponse> updateSlot(
            @PathVariable UUID id,
            @Valid @RequestBody SlotUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(slotService.updateSlot(id, request, authentication.getName()));
    }

    @Operation(summary = "Delete Availability Slot", description = "Allows a tutor to delete an unbooked availability slot.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Slot successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete a booked slot"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the slot owner")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable UUID id,
            Authentication authentication) {

        slotService.deleteSlot(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
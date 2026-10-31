package com.TutorIn.controller;

import com.TutorIn.dto.BookSessionRequest;
import com.TutorIn.dto.SessionResponse;
import com.TutorIn.service.SESSION.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Sessions & Booking", description = "Endpoints for booking, viewing, and canceling tutoring sessions")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @Operation(summary = "Book a Session", description = "Allows an authenticated student to book an available tutor slot.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session successfully booked"),
            @ApiResponse(responseCode = "400", description = "Slot is already booked or not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires STUDENT role")
    })
    @PostMapping
    public ResponseEntity<SessionResponse> bookSession(
            @Valid @RequestBody BookSessionRequest request,
            Authentication authentication) {

        SessionResponse response = sessionService.bookSession(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get Student's Upcoming Sessions", description = "Retrieves all upcoming booked sessions for the authenticated student.")
    @GetMapping("/student")
    public ResponseEntity<List<SessionResponse>> getStudentSessions(Authentication authentication) {
        return ResponseEntity.ok(sessionService.getUpcomingStudentSessions(authentication.getName()));
    }

    @Operation(summary = "Get Tutor's Upcoming Sessions", description = "Retrieves all upcoming booked sessions for the authenticated tutor.")
    @GetMapping("/tutor")
    public ResponseEntity<List<SessionResponse>> getTutorSessions(Authentication authentication) {
        return ResponseEntity.ok(sessionService.getUpcomingTutorSessions(authentication.getName()));
    }

    @Operation(summary = "Cancel a Session", description = "Allows a student or tutor to cancel an upcoming booked session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session successfully cancelled"),
            @ApiResponse(responseCode = "400", description = "Session already cancelled or you do not have permission"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Authentication missing")
    })
    @PutMapping("/{sessionId}/cancel")
    public ResponseEntity<Map<String, String>> cancelSession(
            @PathVariable UUID sessionId,
            Authentication authentication) {

        boolean isTutor = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_TUTOR"));

        sessionService.cancelSession(sessionId, authentication.getName(), isTutor);
        return ResponseEntity.ok(Map.of("message", "Session successfully cancelled."));
    }
}
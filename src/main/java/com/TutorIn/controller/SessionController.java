package com.TutorIn.controller;

import com.TutorIn.dto.BookSessionRequest;
import com.TutorIn.dto.SessionResponse;
import com.TutorIn.service.SESSION.SessionService;
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

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> bookSession(
            @Valid @RequestBody BookSessionRequest request,
            Authentication authentication) {

        SessionResponse response = sessionService.bookSession(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/student")
    public ResponseEntity<List<SessionResponse>> getStudentSessions(Authentication authentication) {
        return ResponseEntity.ok(sessionService.getUpcomingStudentSessions(authentication.getName()));
    }

    @GetMapping("/tutor")
    public ResponseEntity<List<SessionResponse>> getTutorSessions(Authentication authentication) {
        return ResponseEntity.ok(sessionService.getUpcomingTutorSessions(authentication.getName()));
    }

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
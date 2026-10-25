package com.TutorIn.controller;

import com.TutorIn.dto.TutorSearchResponse;
import com.TutorIn.dto.TutorSubjectRequest;
import com.TutorIn.service.TUTORSUBJECT.TutorSubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tutor-subjects")
@RequiredArgsConstructor
public class TutorSubjectController {

    private final TutorSubjectService tutorSubjectService;

    @PostMapping
    public ResponseEntity<Map<String, String>> assignSubject(
            @Valid @RequestBody TutorSubjectRequest request,
            Authentication authentication) {

        tutorSubjectService.assignSubjectToTutor(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Successfully registered to teach this subject."));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TutorSearchResponse>> searchTutors(@RequestParam String title) {
        return ResponseEntity.ok(tutorSubjectService.searchTutorsBySubject(title));
    }
}
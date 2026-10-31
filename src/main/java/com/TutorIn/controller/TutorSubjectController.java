package com.TutorIn.controller;

import com.TutorIn.dto.TutorSearchResponse;
import com.TutorIn.dto.TutorSubjectRequest;
import com.TutorIn.service.TUTORSUBJECT.TutorSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Tutor Subjects", description = "Endpoints for assigning subjects to tutors and searching for tutors by subject")
@RestController
@RequestMapping("/api/v1/tutor-subjects")
@RequiredArgsConstructor
public class TutorSubjectController {

    private final TutorSubjectService tutorSubjectService;

    @Operation(summary = "Assign Subject to Tutor", description = "Allows an authenticated tutor to register to teach a specific subject and set their hourly rate.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully registered to teach this subject"),
            @ApiResponse(responseCode = "400", description = "Subject not found or already assigned"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires TUTOR role")
    })
    @PostMapping
    public ResponseEntity<Map<String, String>> assignSubject(
            @Valid @RequestBody TutorSubjectRequest request,
            Authentication authentication) {

        tutorSubjectService.assignSubjectToTutor(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Successfully registered to teach this subject."));
    }

    @Operation(summary = "Search Tutors by Subject", description = "Allows authenticated users to search for active tutors who teach a specific subject.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of tutors"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TutorSearchResponse>> searchTutors(@RequestParam String title) {
        return ResponseEntity.ok(tutorSubjectService.searchTutorsBySubject(title));
    }
}
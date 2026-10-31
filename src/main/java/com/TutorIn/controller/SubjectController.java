package com.TutorIn.controller;

import com.TutorIn.dto.SubjectRequest;
import com.TutorIn.entity.Subject;
import com.TutorIn.service.SUBJECT.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Subjects", description = "Endpoints for managing the academic subjects available on the platform")
@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @Operation(summary = "Create a Subject", description = "Adds a new academic subject to the platform. Restricted to Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subject successfully created"),
            @ApiResponse(responseCode = "400", description = "Subject already exists or validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    @PostMapping
    public ResponseEntity<Subject> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.createSubject(request));
    }

    @Operation(summary = "Get All Subjects", description = "Retrieves a list of all available subjects on the platform.")
    @GetMapping
    public ResponseEntity<List<Subject>> getAllSubjects() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    @Operation(summary = "Update a Subject", description = "Modifies the title or description of an existing subject. Restricted to Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subject successfully updated"),
            @ApiResponse(responseCode = "400", description = "Another subject with this title already exists"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    @PutMapping("/{subjectId}")
    public ResponseEntity<Subject> updateSubject(
            @PathVariable UUID subjectId,
            @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(subjectId, request));
    }

    @Operation(summary = "Delete a Subject", description = "Removes a subject from the platform. Restricted to Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subject successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete a subject currently assigned to tutors"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    @DeleteMapping("/{subjectId}")
    public ResponseEntity<Map<String, String>> deleteSubject(@PathVariable UUID subjectId) {
        subjectService.deleteSubject(subjectId);
        return ResponseEntity.ok(Map.of("message", "Subject successfully deleted."));
    }
}
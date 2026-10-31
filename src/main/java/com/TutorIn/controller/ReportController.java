package com.TutorIn.controller;

import com.TutorIn.dto.AdminResolveReportRequest;
import com.TutorIn.dto.ReportRequest;
import com.TutorIn.dto.ReportResponse;
import com.TutorIn.service.REPORT.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Reports & Moderation", description = "Endpoints for submitting and managing behavioral reports")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "File a Report", description = "Allows an authenticated student or tutor to report another user for inappropriate behavior.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Report successfully submitted"),
            @ApiResponse(responseCode = "400", description = "Validation error or attempting to report oneself")
    })
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody ReportRequest request,
            Authentication authentication) {

        boolean isTutor = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_TUTOR"));

        ReportResponse response = reportService.createReport(authentication.getName(), isTutor, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get All Reports", description = "Retrieves a list of all system reports. Restricted to Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reports successfully retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    @GetMapping
    public ResponseEntity<List<ReportResponse>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @Operation(summary = "Filter Reports by Status", description = "Retrieves reports matching a specific status (PENDING, IN_REVIEW, RESOLVED, DISMISSED). Restricted to Admins.")
    @GetMapping("/filter/status")
    public ResponseEntity<List<ReportResponse>> getReportsByStatus(@RequestParam String status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    @Operation(summary = "Get Reports Against an Account", description = "Retrieves all reports where the specified email is the accused. Restricted to Admins.")
    @GetMapping("/filter/accused")
    public ResponseEntity<List<ReportResponse>> getReportsAgainstEmail(@RequestParam String email) {
        return ResponseEntity.ok(reportService.getReportsAgainstEmail(email));
    }

    @Operation(summary = "Get Reports Filed by an Account", description = "Retrieves all reports filed by the specified email. Restricted to Admins.")
    @GetMapping("/filter/reporter")
    public ResponseEntity<List<ReportResponse>> getReportsFiledByEmail(@RequestParam String email) {
        return ResponseEntity.ok(reportService.getReportsFiledByEmail(email));
    }

    @Operation(summary = "Resolve a Report", description = "Allows an admin to update the status of a report and append resolution notes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report successfully resolved/updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status or report not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    @PutMapping("/{reportId}/resolve")
    public ResponseEntity<ReportResponse> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody AdminResolveReportRequest request) {

        return ResponseEntity.ok(reportService.resolveReport(reportId, request));
    }
}
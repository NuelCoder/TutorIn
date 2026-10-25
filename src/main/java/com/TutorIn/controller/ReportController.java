package com.TutorIn.controller;

import com.TutorIn.dto.AdminResolveReportRequest;
import com.TutorIn.dto.ReportRequest;
import com.TutorIn.dto.ReportResponse;
import com.TutorIn.service.REPORT.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

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

    @GetMapping
    public ResponseEntity<List<ReportResponse>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/filter/status")
    public ResponseEntity<List<ReportResponse>> getReportsByStatus(@RequestParam String status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    @GetMapping("/filter/accused")
    public ResponseEntity<List<ReportResponse>> getReportsAgainstEmail(@RequestParam String email) {
        return ResponseEntity.ok(reportService.getReportsAgainstEmail(email));
    }

    @GetMapping("/filter/reporter")
    public ResponseEntity<List<ReportResponse>> getReportsFiledByEmail(@RequestParam String email) {
        return ResponseEntity.ok(reportService.getReportsFiledByEmail(email));
    }

    @PutMapping("/{reportId}/resolve")
    public ResponseEntity<ReportResponse> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody AdminResolveReportRequest request) {

        return ResponseEntity.ok(reportService.resolveReport(reportId, request));
    }
}
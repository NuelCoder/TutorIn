package com.TutorIn.controller;

import com.TutorIn.entity.SystemAuditLogs;
import com.TutorIn.service.ADMIN.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Logs", description = "Endpoints for viewing system audit and activity logs")
@RestController
@RequestMapping("api/v1/admins")
@RequiredArgsConstructor
public class AdminLogsController {
    private final AdminService adminService;

    @Operation(summary = "Get System Audit Logs", description = "Retrieves a list of recent system audit logs. Restricted to users with the ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    @GetMapping("/audit-logs")
    public ResponseEntity<List<SystemAuditLogs>> getSystemAuditLogs() {
        List<SystemAuditLogs> logs = adminService.getRecentAuditLogs();
        return ResponseEntity.ok(logs);
    }
}
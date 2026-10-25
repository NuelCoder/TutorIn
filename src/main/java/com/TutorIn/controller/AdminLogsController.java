package com.TutorIn.controller;

import com.TutorIn.entity.SystemAuditLogs;
import com.TutorIn.service.ADMIN.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/admins")
@RequiredArgsConstructor
public class AdminLogsController {
    private final AdminService adminService;

    @GetMapping("/audit-logs")
    public ResponseEntity<List<SystemAuditLogs>> getSystemAuditLogs() {
        List<SystemAuditLogs> logs = adminService.getRecentAuditLogs();
        return ResponseEntity.ok(logs);
    }
}

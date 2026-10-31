package com.TutorIn.controller;

import com.TutorIn.service.ADMIN.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Admin - User Management", description = "Endpoints for admins to manage, suspend, and manually assist users and tutors")
@RestController
@RequestMapping("api/v1/admins")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminService adminService;

    @Operation(summary = "Generate Failover OTP for Student Registration", description = "Manually generates a registration OTP for a student when email delivery fails.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OTP generated successfully"), @ApiResponse(responseCode = "403", description = "Requires ADMIN role")})
    @PostMapping("/users/failover/registration-otp")
    public ResponseEntity<Map<String, Object>> failoverRegistrationOtp(@RequestParam("userEmail") String userEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForUserRegistration(userEmail, currentEmail);

        return ResponseEntity.ok(Map.of(
                "message", "Registration failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @Operation(summary = "Generate Failover OTP for Tutor Registration", description = "Manually generates a registration OTP for a tutor when email delivery fails.")
    @PostMapping("/tutors/failover/registration-otp")
    public ResponseEntity<Map<String, Object>> failoverTutorRegistrationOtp(@RequestParam("tutorEmail") String tutorEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForTutorRegistration(tutorEmail, currentEmail);

        return ResponseEntity.ok(Map.of(
                "message", "Registration failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @Operation(summary = "Generate Failover OTP for Student Forgot Password", description = "Manually generates a password reset OTP for a student.")
    @PostMapping("/users/failover/password-otp")
    public ResponseEntity<Map<String, Object>> failoverPasswordOtp(@RequestParam("userEmail") String userEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForUserForgottenPass(userEmail, currentEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Password change failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @Operation(summary = "Generate Failover OTP for Tutor Forgot Password", description = "Manually generates a password reset OTP for a tutor.")
    @PostMapping("/tutors/failover/password-otp")
    public ResponseEntity<Map<String, Object>> failoverTutorPasswordOtp(@RequestParam("tutorEmail") String tutorEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForTutorForgottenPass(tutorEmail, currentEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Password change failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @Operation(summary = "Generate Failover OTP for Student Password Change", description = "Manually generates an authorized password change OTP for a student.")
    @PostMapping("/users/failover/password-change-otp")
    public ResponseEntity<Map<String, Object>> failoverPasswordChangeOtp(@RequestParam("userEmail") String userEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForUserPasswordChange(userEmail, currentEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Password change failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @Operation(summary = "Generate Failover OTP for Tutor Password Change", description = "Manually generates an authorized password change OTP for a tutor.")
    @PostMapping("/tutors/failover/password-change-otp")
    public ResponseEntity<Map<String, Object>> failoverTutorPasswordChangeOtp(@RequestParam("tutorEmail") String tutorEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForTutorPasswordChange(tutorEmail, currentEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Password change failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @Operation(summary = "Admin Override: Change Student Email", description = "Forcefully updates a student's email address from the admin panel.")
    @PutMapping("/users/change-email")
    public ResponseEntity<Map<String, String>> adminChangeUserEmail(
            @RequestParam("currentEmail") String currentEmail,
            @RequestParam("newEmail") String newEmail) {

        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.adminOverrideUserEmail(currentEmail, newEmail, currentAdminEmail);
        return ResponseEntity.ok(Map.of("message", "User email successfully updated from the admin panel."));
    }

    @Operation(summary = "Admin Override: Change Tutor Email", description = "Forcefully updates a tutor's email address from the admin panel.")
    @PutMapping("/tutors/change-email")
    public ResponseEntity<Map<String, String>> adminChangeTutorEmail(
            @RequestParam("currentEmail") String currentEmail,
            @RequestParam("newEmail") String newEmail) {

        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.adminOverrideTutorEmail(currentEmail, newEmail, currentAdminEmail);
        return ResponseEntity.ok(Map.of("message", "Tutor email successfully updated from the admin panel."));
    }

    @Operation(summary = "Suspend Student Account", description = "Suspends a student account, blocking them from logging in or booking sessions.")
    @PutMapping("/users/suspend")
    public ResponseEntity<Map<String, String>> suspendUser(@RequestParam("userEmail") String userEmail) {
        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.toggleUserSuspension(userEmail, true, currentAdminEmail);
        return ResponseEntity.ok(Map.of("message", "User account has been successfully suspended."));
    }

    @Operation(summary = "Suspend Tutor Account", description = "Suspends a tutor account, blocking them from logging in or offering sessions.")
    @PutMapping("/tutors/suspend")
    public ResponseEntity<Map<String, String>> suspendTutor(@RequestParam("tutorEmail") String tutorEmail) {
        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.toggleTutorSuspension(tutorEmail, true, currentAdminEmail);
        return ResponseEntity.ok(Map.of("message", "Tutor account has been successfully suspended."));
    }

    @Operation(summary = "Reactivate Student Account", description = "Restores access to a suspended student account.")
    @PutMapping("/users/reactivate")
    public ResponseEntity<Map<String, String>> reactivateUser(@RequestParam("userEmail") String userEmail) {
        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.toggleUserSuspension(userEmail, false, currentAdminEmail);
        return ResponseEntity.ok(Map.of("message", "User account has been successfully reactivated."));
    }

    @Operation(summary = "Reactivate Tutor Account", description = "Restores access to a suspended tutor account.")
    @PutMapping("/tutors/reactivate")
    public ResponseEntity<Map<String, String>> reactivateTutor(@RequestParam("tutorEmail") String tutorEmail) {
        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.toggleTutorSuspension(tutorEmail, false, currentAdminEmail);
        return ResponseEntity.ok(Map.of("message", "Tutor account has been successfully reactivated."));
    }
}
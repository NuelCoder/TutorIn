package com.TutorIn.controller;

import com.TutorIn.service.ADMIN.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/admins")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminService adminService;

    @PostMapping("/users/failover/registration-otp")
    public ResponseEntity<Map<String, Object>> failoverRegistrationOtp(@RequestParam("userEmail") String userEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForUserRegistration(userEmail, currentEmail);

        return ResponseEntity.ok(Map.of(
                "message", "Registration failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @PostMapping("/tutors/failover/registration-otp")
    public ResponseEntity<Map<String, Object>> failoverTutorRegistrationOtp(@RequestParam("tutorEmail") String tutorEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForTutorRegistration(tutorEmail, currentEmail);

        return ResponseEntity.ok(Map.of(
                "message", "Registration failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @PostMapping("/users/failover/password-otp")
    public ResponseEntity<Map<String, Object>> failoverPasswordOtp(@RequestParam("userEmail") String userEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForUserForgottenPass(userEmail, currentEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Password change failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @PostMapping("/tutors/failover/password-otp")
    public ResponseEntity<Map<String, Object>> failoverTutorPasswordOtp(@RequestParam("tutorEmail") String tutorEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForTutorForgottenPass(tutorEmail, currentEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Password change failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @PostMapping("/users/failover/password-change-otp")
    public ResponseEntity<Map<String, Object>> failoverPasswordChangeOtp(@RequestParam("userEmail") String userEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForUserPasswordChange(userEmail, currentEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Password change failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @PostMapping("/tutors/failover/password-change-otp")
    public ResponseEntity<Map<String, Object>> failoverTutorPasswordChangeOtp(@RequestParam("tutorEmail") String tutorEmail) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String otp = adminService.adminGenerateManualOtpForTutorPasswordChange(tutorEmail, currentEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Password change failover OTP generated successfully.",
                "otp", otp
        ));
    }

    @PutMapping("/users/change-email")
    public ResponseEntity<Map<String, String>> adminChangeUserEmail(
            @RequestParam("currentEmail") String currentEmail,
            @RequestParam("newEmail") String newEmail) {

        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.adminOverrideUserEmail(currentEmail, newEmail, currentAdminEmail);
        return ResponseEntity.ok(Map.of("message", "User email successfully updated from the admin panel."));
    }

    @PutMapping("/tutors/change-email")
    public ResponseEntity<Map<String, String>> adminChangeTutorEmail(
            @RequestParam("currentEmail") String currentEmail,
            @RequestParam("newEmail") String newEmail) {

        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.adminOverrideTutorEmail(currentEmail, newEmail, currentAdminEmail);
        return ResponseEntity.ok(Map.of("message", "Tutor email successfully updated from the admin panel."));
    }

    @PutMapping("/users/suspend")
    public ResponseEntity<Map<String, String>> suspendUser(@RequestParam("userEmail") String userEmail) {
        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.toggleUserSuspension(userEmail, true, currentAdminEmail);

        return ResponseEntity.ok(Map.of("message", "User account has been successfully suspended."));
    }

    @PutMapping("/tutors/suspend")
    public ResponseEntity<Map<String, String>> suspendTutor(@RequestParam("tutorEmail") String tutorEmail) {
        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.toggleTutorSuspension(tutorEmail, true, currentAdminEmail);

        return ResponseEntity.ok(Map.of("message", "Tutor account has been successfully suspended."));
    }

    @PutMapping("/users/reactivate")
    public ResponseEntity<Map<String, String>> reactivateUser(@RequestParam("userEmail") String userEmail) {
        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.toggleUserSuspension(userEmail, false, currentAdminEmail);

        return ResponseEntity.ok(Map.of("message", "User account has been successfully reactivated."));
    }

    @PutMapping("/tutors/reactivate")
    public ResponseEntity<Map<String, String>> reactivateTutor(@RequestParam("tutorEmail") String tutorEmail) {
        String currentAdminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.toggleTutorSuspension(tutorEmail, false, currentAdminEmail);

        return ResponseEntity.ok(Map.of("message", "Tutor account has been successfully reactivated."));
    }
}

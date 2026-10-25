package com.TutorIn.controller;

import com.TutorIn.dto.*;
import com.TutorIn.entity.Admin;
import com.TutorIn.entity.PendingAdmin;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.ADMIN.AdminService;
import com.TutorIn.service.PENDINGADMIN.PendingAdminService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/admins")
@RequiredArgsConstructor
public class AdminController {
    private final PendingAdminService pendingAdminService;
    private final AdminService adminService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${EXPIRY_DAYS}")
    private Long EXPIRY_DAYS;

    @Value("${app.cookie.secure:false}")
    private boolean isCookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/register-admin")
    public ResponseEntity<Map<String,Object>> registerAdmin(@Valid @RequestBody PendingAdminDTO pendingAdminDTO){
        PendingAdmin pendingAdmin = pendingAdminService.registerAdmin(pendingAdminDTO);
        AdminProfileResponse adminProfileResponse = AdminProfileResponse.builder()
                .adminName(pendingAdmin.getAdminName())
                .adminEmail(pendingAdmin.getAdminEmail())
                .role(pendingAdmin.getRole())
                .createdAt(LocalDateTime.now())
                .build();

        Map<String,Object> response = new HashMap<>();
        response.put("message", "Profile has been filled");
        response.put("pending", adminProfileResponse);
        response.put("alert", "OTP has been sent to: "+ pendingAdminDTO.getAdminEmail());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-admin")
    public ResponseEntity<Map<String, Object>> verifyAdmin(@Valid @RequestBody OtpVerificationRequest request) {

        adminService.verifyOtpAndRegisterUser(request);

        String token = jwtUtil.generateToken(request.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(request.getEmail());

        ResponseCookie refreshCookie = generateSecureCookie(refreshToken);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP has been verified and registration is successful");
        response.put("token", token);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AdminProfileResponse> getAdmin(){
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Admin admin = adminService.getAdminProfile(currentEmail);
        AdminProfileResponse adminProfileResponse = AdminProfileResponse.builder()
                .adminName(admin.getAdminName())
                .adminEmail(admin.getAdminEmail())
                .role(admin.getRole())
                .build();
        return ResponseEntity.ok(adminProfileResponse);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String,Object>> deleteAdmin(){
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.deleteAdminProfile(currentEmail);
        Map<String,Object> response = new HashMap<>();
        response.put("message", "Account successfully deleted. We are sad to see you go");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String,Object>> updateAdmin(@Valid @RequestBody UpdateAdmin updateAdmin){
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Admin updatedAdmin = adminService.updateAdminProfile(currentEmail,updateAdmin);
        AdminProfileResponse adminProfileResponse = AdminProfileResponse.builder()
                .adminName(updatedAdmin.getAdminName())
                .adminEmail(updatedAdmin.getAdminEmail())
                .role(updatedAdmin.getRole())
                .build();

        Map<String,Object> response = new HashMap<>();

        response.put("message", "Your details have been updated");

        response.put("admin",adminProfileResponse);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-registration-otp")
    public ResponseEntity<Map<String, Object>> resendAdminRegistrationOtp(@RequestBody Map<String, String> request) {

        adminService.resendAdminRegistrationOtp(request.get("adminEmail"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-password-change-otp")
    public ResponseEntity<Map<String, Object>> resendAdminPasswordChangeOtp() {

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.resendAdminPasswordChangeOTP(adminEmail);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-admin-password/initiate")
    public ResponseEntity<Map<String, Object>> initiateAdminPasswordChange(@Valid @RequestBody PasswordChangeRequest request) {

        adminService.initiateAdminPasswordChange(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "An OTP has been sent to your email to authorize this password change.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-admin-password/verify")
    public ResponseEntity<Map<String, Object>> verifyAdminPasswordChange(@Valid @RequestBody PasswordVerificationRequest request) {

        adminService.validateAdminPasswordChange(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Your password has been successfully updated.");
        return ResponseEntity.ok(response);
    }

    private ResponseCookie generateSecureCookie(String refreshToken) {
        long durationDays = EXPIRY_DAYS;
        long maxAgeSeconds = durationDays * 24 * 60 * 60;

        return ResponseCookie.from("admin_refresh_token", refreshToken)
                .httpOnly(true)
                .secure(isCookieSecure)
                .sameSite(cookieSameSite)
                .path("/") // 🚨 Fixed the path to match the class mapping!
                .maxAge(maxAgeSeconds)
                .build();
    }
}

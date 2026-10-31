package com.TutorIn.controller;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.LoginResponse;
import com.TutorIn.service.ADMIN.AdminService;
import com.TutorIn.service.LOGIN.AdminAccountRecoveryService;
import com.TutorIn.service.LOGIN.TutorAccountRecoveryService;
import com.TutorIn.service.LOGIN.UserAccountRecoveryService;
import com.TutorIn.service.TUTOR.TutorService;
import com.TutorIn.service.USER.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Account Recovery", description = "Endpoints for recovering recently deleted accounts via OTP")
@RestController
@RequestMapping("api/v1/auth/recovery")
@RequiredArgsConstructor
public class AccountRecoveryController {

    private final UserAccountRecoveryService userAccountRecoveryService;
    private final TutorAccountRecoveryService tutorAccountRecoveryService;
    private final AdminAccountRecoveryService adminAccountRecoveryService;
    private final UserService userService;
    private final TutorService tutorService;
    private final AdminService adminService;

    @Value("${EXPIRY_DAYS}")
    private Long EXPIRY_DAYS;

    @Value("${app.cookie.secure:false}")
    private boolean isCookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Operation(summary = "Initiate Student Account Recovery", description = "Triggers an OTP email for a student recovering a deactivated account.")
    @ApiResponse(responseCode = "200", description = "Recovery initiated successfully")
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> initiateAccountRecovery(@RequestBody Map<String, String> request) {
        userAccountRecoveryService.initiateRecovery(request.get("email"));
        return ResponseEntity.ok(Map.of("message", "If an account exists, a recovery OTP has been sent to your email."));
    }

    @Operation(summary = "Confirm Student Account Recovery", description = "Validates the OTP and logs the student in, returning a JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account recovered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    @PostMapping("/confirm")
    public ResponseEntity<LoginResponse> confirmAccountRecovery(@RequestBody Map<String, String> request) {
        AuthTokens tokens = userAccountRecoveryService.confirmRecovery(request.get("email"), request.get("otp"));
        ResponseCookie refreshCookie = generateSecureCookie("user_refresh_token", tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(tokens.accessToken()));
    }

    @Operation(summary = "Initiate Tutor Account Recovery", description = "Triggers an OTP email for a tutor recovering a deactivated account.")
    @PostMapping("/tutor/initiate")
    public ResponseEntity<Map<String, String>> initiateTutorAccountRecovery(@RequestBody Map<String, String> request) {
        tutorAccountRecoveryService.initiateRecovery(request.get("email"));
        return ResponseEntity.ok(Map.of("message", "If an account exists, a recovery OTP has been sent to your email."));
    }

    @Operation(summary = "Confirm Tutor Account Recovery", description = "Validates the OTP and logs the tutor in, returning a JWT.")
    @PostMapping("/tutor/confirm")
    public ResponseEntity<LoginResponse> confirmTutorAccountRecovery(@RequestBody Map<String, String> request) {
        AuthTokens tokens = tutorAccountRecoveryService.confirmRecovery(request.get("email"), request.get("otp"));
        ResponseCookie refreshCookie = generateSecureCookie("tutor_refresh_token", tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(tokens.accessToken()));
    }

    @Operation(summary = "Initiate Admin Account Recovery", description = "Triggers an OTP email for an admin recovering a deactivated account.")
    @PostMapping("/admin/initiate")
    public ResponseEntity<Map<String, String>> initiateAdminAccountRecovery(@RequestBody Map<String, String> request) {
        adminAccountRecoveryService.initiateRecovery(request.get("adminEmail"));
        return ResponseEntity.ok(Map.of("message", "If an account exists, a recovery OTP has been sent to your email."));
    }

    @Operation(summary = "Confirm Admin Account Recovery", description = "Validates the OTP and logs the admin in, returning a JWT.")
    @PostMapping("/admin/confirm")
    public ResponseEntity<LoginResponse> confirmAdminAccountRecovery(@RequestBody Map<String, String> request) {
        AuthTokens tokens = adminAccountRecoveryService.confirmRecovery(request.get("adminEmail"), request.get("otp"));
        ResponseCookie refreshCookie = generateSecureCookie("admin_refresh_token", tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(tokens.accessToken()));
    }

    @Operation(summary = "Resend Student Recovery OTP")
    @PostMapping("/resend-recovery-otp")
    public ResponseEntity<Map<String, Object>> resendRecoveryOtp(@RequestBody Map<String, String> request) {
        userService.resendRecoveryOTP(request.get("email"));
        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Resend Tutor Recovery OTP")
    @PostMapping("/resend-tutor-recovery-otp")
    public ResponseEntity<Map<String, Object>> resendTutorRecoveryOtp(@RequestBody Map<String, String> request) {
        tutorService.resendRecoveryOTP(request.get("email"));
        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Resend Admin Recovery OTP")
    @PostMapping("/resend-admin-recovery-otp")
    public ResponseEntity<Map<String, Object>> resendAdminRecoveryOtp(@RequestBody Map<String, String> request) {
        adminService.resendAdminRecoveryOTP(request.get("email"));
        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");
        return ResponseEntity.ok(response);
    }

    private ResponseCookie generateSecureCookie(String cookieName, String refreshToken) {
        long durationDays = EXPIRY_DAYS;
        long maxAgeSeconds = durationDays * 24 * 60 * 60; // Fixed syntax error here

        return ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(isCookieSecure)
                .sameSite(cookieSameSite)
                .path("/") // Set to root so the browser sends it correctly
                .maxAge(maxAgeSeconds)
                .build();
    }
}
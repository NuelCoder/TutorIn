package com.TutorIn.controller;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.LoginResponse;
import com.TutorIn.service.ADMIN.AdminService;
import com.TutorIn.service.LOGIN.AdminAccountRecoveryService;
import com.TutorIn.service.LOGIN.TutorAccountRecoveryService;
import com.TutorIn.service.LOGIN.UserAccountRecoveryService;
import com.TutorIn.service.TUTOR.TutorService;
import com.TutorIn.service.USER.UserService;
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

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> initiateAccountRecovery(@RequestBody Map<String, String> request) {
        userAccountRecoveryService.initiateRecovery(request.get("email"));

        return ResponseEntity.ok(Map.of(
                "message", "If an account exists, a recovery OTP has been sent to your email."
        ));
    }

    @PostMapping("/confirm")
    public ResponseEntity<LoginResponse> confirmAccountRecovery(@RequestBody Map<String, String> request) {

        AuthTokens tokens = userAccountRecoveryService.confirmRecovery(request.get("email"), request.get("otp"));

        ResponseCookie refreshCookie = generateSecureCookie("user_refresh_token", tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(tokens.accessToken()));
    }

    @PostMapping("/tutor/initiate")
    public ResponseEntity<Map<String, String>> initiateTutorAccountRecovery(@RequestBody Map<String, String> request) {
        tutorAccountRecoveryService.initiateRecovery(request.get("email"));

        return ResponseEntity.ok(Map.of(
                "message", "If an account exists, a recovery OTP has been sent to your email."
        ));
    }

    @PostMapping("/tutor/confirm")
    public ResponseEntity<LoginResponse> confirmTutorAccountRecovery(@RequestBody Map<String, String> request) {

        AuthTokens tokens = tutorAccountRecoveryService.confirmRecovery(request.get("email"), request.get("otp"));

        ResponseCookie refreshCookie = generateSecureCookie("tutor_refresh_token", tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(tokens.accessToken()));
    }

    @PostMapping("/admin/initiate")
    public ResponseEntity<Map<String, String>> initiateAdminAccountRecovery(@RequestBody Map<String, String> request) {
        adminAccountRecoveryService.initiateRecovery(request.get("adminEmail"));

        return ResponseEntity.ok(Map.of(
                "message", "If an account exists, a recovery OTP has been sent to your email."
        ));
    }

    @PostMapping("/admin/confirm")
    public ResponseEntity<LoginResponse> confirmAdminAccountRecovery(@RequestBody Map<String, String> request) {

        AuthTokens tokens = adminAccountRecoveryService.confirmRecovery(request.get("adminEmail"), request.get("otp"));

        ResponseCookie refreshCookie = generateSecureCookie("admin_refresh_token", tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(tokens.accessToken()));
    }

    @PostMapping("/resend-recovery-otp")
    public ResponseEntity<Map<String, Object>> resendRecoveryOtp(@RequestBody Map<String, String> request) {

        userService.resendRecoveryOTP(request.get("email"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-tutor-recovery-otp")
    public ResponseEntity<Map<String, Object>> resendTutorRecoveryOtp(@RequestBody Map<String, String> request) {

        tutorService.resendRecoveryOTP(request.get("email"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-admin-recovery-otp")
    public ResponseEntity<Map<String, Object>> resendAdminRecoveryOtp(@RequestBody Map<String, String> request) {

        adminService.resendAdminRecoveryOTP(request.get("email"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    private ResponseCookie generateSecureCookie(String cookieName, String refreshToken) {
        long durationDays = EXPIRY_DAYS;
        long maxAgeSeconds = durationDays * 24 * 60 * 60;

        return ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(isCookieSecure)
                .sameSite(cookieSameSite)
                .path("/") // Set to root so the browser sends it correctly
                .maxAge(maxAgeSeconds)
                .build();
    }
}

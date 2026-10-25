package com.TutorIn.controller;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.LoginRequest;
import com.TutorIn.dto.LoginResponse;
import com.TutorIn.dto.PasswordVerificationRequest;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.ADMIN.AdminService;
import com.TutorIn.service.LOGIN.AdminLoginService;
import com.TutorIn.service.LOGIN.TutorLoginService;
import com.TutorIn.service.LOGIN.UserLoginService;
import com.TutorIn.service.LOGOUT.LogoutService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import com.TutorIn.service.TUTOR.TutorService;
import com.TutorIn.service.USER.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserLoginService userLoginService;
    private final AdminLoginService adminLoginService;
    private final TutorLoginService tutorLoginService;
    private final LogoutService logoutService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final AdminService adminService;
    private final TutorService tutorService;

    @Value("${EXPIRY_DAYS}")
    private Long EXPIRY_DAYS;

    @Value("${app.cookie.secure:false}")
    private boolean isCookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/user-login")
    public ResponseEntity<LoginResponse> userLogin(@RequestBody LoginRequest loginRequest) {
        AuthTokens tokens = userLoginService.loginUser(loginRequest);
        ResponseCookie responseCookie = generateSecureCookie("user_refresh_token",tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(new LoginResponse(tokens.accessToken()));
    }

    @PostMapping("/tutor-login")
    public ResponseEntity<LoginResponse> tutorLogin(@RequestBody LoginRequest loginRequest) {
        AuthTokens tokens = tutorLoginService.loginTutor(loginRequest);
        ResponseCookie responseCookie = generateSecureCookie("tutor_refresh_token",tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(new LoginResponse(tokens.accessToken()));
    }

    @PostMapping("/admin-login")
    public ResponseEntity<LoginResponse> adminLogin(@RequestBody LoginRequest loginRequest) {
        AuthTokens tokens = adminLoginService.loginAdmin(loginRequest);
        ResponseCookie responseCookie = generateSecureCookie("admin_refresh_token",tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(new LoginResponse(tokens.accessToken()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> userForgotPassword(@RequestBody Map<String, String> request) {

        userService.initiateForgotPassword(request.get("email"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "An OTP has been sent to your inbox.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-tutor-password")
    public ResponseEntity<Map<String, Object>> tutorForgotPassword(@RequestBody Map<String, String> request) {

        tutorService.initiateForgotPassword(request.get("email"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "An OTP has been sent to your inbox.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-admin-password")
    public ResponseEntity<Map<String,Object>> adminForgotPassword(@RequestBody Map<String, String> request){

        adminService.initiateAdminForgotPassword(request.get("adminEmail"));

        Map<String,Object> response = new HashMap<>();
        response.put("message", "An OTP has been sent to your inbox.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-forgotten-password-otp")
    public ResponseEntity<Map<String, Object>> resendForgottenPasswordOtp(@RequestBody Map<String, String> request) {

        userService.resendForgotPasswordOTP(request.get("email"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-tutor-forgotten-password-otp")
    public ResponseEntity<Map<String, Object>> resendTutorForgottenPasswordOtp(@RequestBody Map<String, String> request) {

        tutorService.resendForgotPasswordOTP(request.get("email"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-admin-forgotten-password-otp")
    public ResponseEntity<Map<String, Object>> resendAdminForgottenPasswordOtp(@RequestBody Map<String, String> request) {

        adminService.resendAdminForgotPasswordOTP(request.get("adminEmail"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> userResetPassword(@Valid @RequestBody PasswordVerificationRequest request) {

        userService.resetForgottenPassword(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Your password has been successfully reset. You can now log in.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-tutor-password")
    public ResponseEntity<Map<String, Object>> tutorResetPassword(@Valid @RequestBody PasswordVerificationRequest request) {

        userService.resetForgottenPassword(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Your password has been successfully reset. You can now log in.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-admin-password")
    public ResponseEntity<Map<String, Object>> adminResetPassword(@Valid @RequestBody PasswordVerificationRequest request){
        adminService.resetAdminForgottenPassword(request);

        Map<String,Object> response = new HashMap<>();
        response.put("message", "Your password has been successfully reset. You can now log in.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user-refresh")
    public ResponseEntity<LoginResponse> refreshUserToken(
            @CookieValue(name = "user_refresh_token", required = false) String incomingRefreshToken) throws BadRequestException {

        // 1. Pass the cookie and explicitly demand the USER role
        String newAccessToken = refreshTokenService.processRefreshToken(incomingRefreshToken, Role.USER);

        // 2. Return the brand new 15-minute token
        return ResponseEntity.ok(new LoginResponse(newAccessToken));
    }

    @PostMapping("/tutor-refresh")
    public ResponseEntity<LoginResponse> refreshTutorToken(
            @CookieValue(name = "tutor_refresh_token", required = false) String incomingRefreshToken) throws BadRequestException {

        // 1. Pass the cookie and explicitly demand the USER role
        String newAccessToken = refreshTokenService.processRefreshToken(incomingRefreshToken, Role.TUTOR);

        // 2. Return the brand new 15-minute token
        return ResponseEntity.ok(new LoginResponse(newAccessToken));
    }

    @PostMapping("/admin-refresh")
    public ResponseEntity<LoginResponse> refreshAdminToken(
            @CookieValue(name = "admin_refresh_token", required = false) String incomingRefreshToken) throws BadRequestException {

        // 1. Pass the cookie and explicitly demand the ADMIN role
        String newAccessToken = refreshTokenService.processRefreshToken(incomingRefreshToken, Role.ADMIN);

        // 2. Return the brand new 15-minute token
        return ResponseEntity.ok(new LoginResponse(newAccessToken));
    }


    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader,
                                                      @CookieValue(name = "user_refresh_token", required = false) String userRefreshToken,
                                                      @CookieValue(name = "admin_refresh_token", required = false) String adminRefreshToken,
                                                      @CookieValue(name = "tutor_refresh_token", required = false) String tutorRefreshToken)
    {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Missing or invalid Authorization header");
        }

        String jwt = authHeader.substring(7);
        logoutService.logout(jwt);

        if (userRefreshToken != null) refreshTokenService.revokeSingleToken(userRefreshToken);
        if (tutorRefreshToken != null) refreshTokenService.revokeSingleToken(tutorRefreshToken);
        if (adminRefreshToken != null) refreshTokenService.revokeSingleToken(adminRefreshToken);

        // 🚨 Kill both potential cookies immediately
        ResponseCookie clearUser = buildClearCookie("user_refresh_token");
        ResponseCookie clearTutor = buildClearCookie("tutor_refresh_token");
        ResponseCookie clearAdmin = buildClearCookie("admin_refresh_token");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logout successful.");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearUser.toString())
                .header(HttpHeaders.SET_COOKIE, clearTutor.toString())
                .header(HttpHeaders.SET_COOKIE, clearAdmin.toString())
                .body(response);
    }

    private ResponseCookie generateSecureCookie(String cookieName, String refreshToken) {
        long durationDays = EXPIRY_DAYS;
        long maxAgeSeconds = durationDays * 24 * 60 * 60;

        return ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(isCookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    // 🚨 Helper for clearing
    private ResponseCookie buildClearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(isCookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
    }
}

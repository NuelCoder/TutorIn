package com.TutorIn.controller;

import com.TutorIn.dto.*;
import com.TutorIn.entity.PendingUser;
import com.TutorIn.entity.User;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.PENDINGUSER.PendingUserService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import com.TutorIn.service.USER.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final PendingUserService pendingUserService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${EXPIRY_DAYS}")
    private Long EXPIRY_DAYS;

    @Value("${app.cookie.secure:false}")
    private boolean isCookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;


    @PostMapping("/register-user")
    public ResponseEntity<Map<String,Object>> registerUser(@Valid @RequestBody PendingUserDTO pendingUserDTO) {
        PendingUser pendingUser = pendingUserService.registerUser(pendingUserDTO);
        UserProfileResponse profileResponse = UserProfileResponse.builder()
                .userId(pendingUser.getPendingUserId())
                .name(pendingUser.getName())
                .email(pendingUser.getEmail())
                .gender(pendingUser.getGender())
                .country(pendingUser.getCountry())
                .role(pendingUser.getRole())
                .createdAt(pendingUser.getCreatedAt())
                .build();

        Map<String,Object> response = new HashMap<>();
        response.put("message", "Profile has been filled");
        response.put("pending", profileResponse);
        response.put("alert", "OTP has been sent to: "+ pendingUserDTO.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-user")
    public ResponseEntity<Map<String, Object>> verifyUser(@Valid @RequestBody OtpVerificationRequest request) {

        userService.verifyOtpAndRegisterUser(request);

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
    public ResponseEntity<UserProfileResponse> getUser(){
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserProfile(currentEmail);
        UserProfileResponse profileResponse = UserProfileResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .gender(user.getGender())
                .country(user.getCountry())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(profileResponse);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String,Object>> deleteUser (){

        String currentEmail= SecurityContextHolder.getContext().getAuthentication().getName();

        userService.deleteUserProfile(currentEmail);

        Map<String,Object> response = new HashMap<>();

        response.put("message", "Account successfully deleted. We are sad to see you go");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String,Object>> updateUser(@Valid @RequestBody UpdateUser updateUser){
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User updatedUser = userService.updateUserProfile(currentEmail, updateUser);

        UserProfileResponse profileResponse = UserProfileResponse.builder()
                .userId(updatedUser.getUserId())
                .name(updatedUser.getName())
                .email(updatedUser.getEmail())
                .gender(updatedUser.getGender())
                .role(updatedUser.getRole())
                .createdAt(updatedUser.getCreatedAt())
                .build();

        Map<String, Object> response = new HashMap<>();

        response.put("message", "Your details have been updated");

        response.put("user",profileResponse);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password/initiate")
    public ResponseEntity<Map<String, Object>> initiatePasswordChange(@Valid @RequestBody PasswordChangeRequest request) {

        userService.initiatePasswordChange(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "An OTP has been sent to your email to authorize this password change.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password/verify")
    public ResponseEntity<Map<String, Object>> verifyPasswordChange(@Valid @RequestBody PasswordVerificationRequest request) {

        userService.validatePasswordChange(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Your password has been successfully updated.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-registration-otp")
    public ResponseEntity<Map<String, Object>> resendRegistrationOtp(@RequestBody Map<String, String> request) {

        userService.resendRegistrationOtp(request.get("email"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-password-change-otp")
    public ResponseEntity<Map<String, Object>> resendPasswordChangeOtp() {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        userService.resendPasswordChangeOTP(email);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    private ResponseCookie generateSecureCookie(String refreshToken) {
        long durationDays = EXPIRY_DAYS;
        long maxAgeSeconds = durationDays * 24 * 60 * 60;

        return ResponseCookie.from("user_refresh_token", refreshToken)
                .httpOnly(true)
                .secure(isCookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}

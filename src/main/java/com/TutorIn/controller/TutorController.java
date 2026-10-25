package com.TutorIn.controller;

import com.TutorIn.dto.*;
import com.TutorIn.entity.PendingTutor;
import com.TutorIn.entity.PendingUser;
import com.TutorIn.entity.Tutor;
import com.TutorIn.entity.User;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.PENDINGTUTOR.PendingTutorService;
import com.TutorIn.service.PENDINGUSER.PendingUserService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import com.TutorIn.service.TUTOR.TutorService;
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
@RequestMapping("api/v1/tutors")
@RequiredArgsConstructor
public class TutorController {
    private final TutorService tutorService;
    private final PendingTutorService pendingTutorService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${EXPIRY_DAYS}")
    private Long EXPIRY_DAYS;

    @Value("${app.cookie.secure:false}")
    private boolean isCookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;


    @PostMapping("/register-tutor")
    public ResponseEntity<Map<String,Object>> registerTutor(@Valid @RequestBody PendingTutorDTO pendingTutorDTO) {
        PendingTutor pendingTutor= pendingTutorService.registerTutor(pendingTutorDTO);
        TutorProfileResponse profileResponse = TutorProfileResponse.builder()
                .tutorId(pendingTutor.getPendingTutorId())
                .bio(pendingTutor.getBio())
                .email(pendingTutor.getEmail())
                .gender(pendingTutor.getGender())
                .country(pendingTutor.getCountry())
                .role(pendingTutor.getRole())
                .createdAt(pendingTutor.getCreatedAt())
                .build();

        Map<String,Object> response = new HashMap<>();
        response.put("message", "Profile has been filled");
        response.put("pending", profileResponse);
        response.put("alert", "OTP has been sent to: "+ pendingTutorDTO.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-tutor")
    public ResponseEntity<Map<String, Object>> verifyTutor(@Valid @RequestBody OtpVerificationRequest request) {

        tutorService.verifyOtpAndRegisterUser(request);

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
    public ResponseEntity<TutorProfileResponse> getTutor(){
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Tutor tutor = tutorService.getTutorProfile(currentEmail);
        TutorProfileResponse profileResponse = TutorProfileResponse.builder()
                .tutorId(tutor.getTutorId())
                .name(tutor.getName())
                .bio(tutor.getBio())
                .email(tutor.getEmail())
                .gender(tutor.getGender())
                .country(tutor.getCountry())
                .role(tutor.getRole())
                .createdAt(tutor.getCreatedAt())
                .build();

        return ResponseEntity.ok(profileResponse);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String,Object>> deleteTutor (){

        String currentEmail= SecurityContextHolder.getContext().getAuthentication().getName();

        tutorService.deleteTutorProfile(currentEmail);

        Map<String,Object> response = new HashMap<>();

        response.put("message", "Account successfully deleted. We are sad to see you go");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String,Object>> updateUser(@Valid @RequestBody UpdateTutor updateTutor){
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Tutor updatedTutor= tutorService.updateTutorProfile(currentEmail, updateTutor);

        TutorProfileResponse profileResponse = TutorProfileResponse.builder()
                .tutorId(updatedTutor.getTutorId())
                .name(updatedTutor.getName())
                .bio(updatedTutor.getBio())
                .email(updatedTutor.getEmail())
                .gender(updatedTutor.getGender())
                .role(updatedTutor.getRole())
                .createdAt(updatedTutor.getCreatedAt())
                .build();

        Map<String, Object> response = new HashMap<>();

        response.put("message", "Your details have been updated");

        response.put("tutor",profileResponse);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password/initiate")
    public ResponseEntity<Map<String, Object>> initiatePasswordChange(@Valid @RequestBody PasswordChangeRequest request) {

        tutorService.initiatePasswordChange(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "An OTP has been sent to your email to authorize this password change.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password/verify")
    public ResponseEntity<Map<String, Object>> verifyPasswordChange(@Valid @RequestBody PasswordVerificationRequest request) {

        tutorService.validatePasswordChange(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Your password has been successfully updated.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-registration-otp")
    public ResponseEntity<Map<String, Object>> resendRegistrationOtp(@RequestBody Map<String, String> request) {

        tutorService.resendRegistrationOtp(request.get("email"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "A fresh OTP has been sent to your email.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-password-change-otp")
    public ResponseEntity<Map<String, Object>> resendPasswordChangeOtp() {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        tutorService.resendPasswordChangeOTP(email);

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

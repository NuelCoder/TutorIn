package com.TutorIn.service.OTP;

import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TutorRepository tutorRepository;
    private final PendingAdminRepository pendingAdminRepository;
    private final PendingUserRepository pendingUserRepository;
    private final PendingTutorRepository pendingTutorRepository;

    // Pulling dynamic configuration directly from application.yml
    @Value("${otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${otp.length:6}")
    private int otpLength;

    private static final String OTP_KEY = "otp:";
    private static final String ATTEMPT_KEY = "otp_attempts:";
    private static final String COOLDOWN_KEY = "otp_cooldown:";

    public String generateRegistrationOTP(String email) {
        if (pendingUserRepository.findByEmail(email).isEmpty()) {
            throw new BadRequestException("No pending user found for this email: " + email);
        }
        return createAndStoreOtp(email);
    }

    public String generateAdminRegistrationOTP(String adminEmail){
        if (pendingAdminRepository.findByAdminEmail(adminEmail).isEmpty()){
            throw new BadRequestException("No pending admin found for this email: " + adminEmail);
        }
        return createAndStoreOtp(adminEmail);
    }

    public String generateTutorRegistrationOTP(String email){
        if (pendingTutorRepository.findByEmail(email).isEmpty()){
            throw new BadRequestException("No pending tutor found for this email: " + email);
        }
        return createAndStoreOtp(email);
    }

    public String generateForgottenPasswordOTP(String email) {
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new BadRequestException("No user found for this email: " + email);
        }
        return createAndStoreOtp(email);
    }

    public String generateAdminForgottenPasswordOTP(String adminEmail){
        if (adminRepository.findByAdminEmail(adminEmail).isEmpty()){
            throw new BadRequestException("No admin found for this email: " + adminEmail);
        }
        return createAndStoreOtp(adminEmail);
    }

    public String generateTutorForgottenPasswordOTP(String email){
        if (tutorRepository.findByEmail(email).isEmpty()){
            throw new BadRequestException("No tutor found for this email: " + email);
        }
        return createAndStoreOtp(email);
    }

    public String generateRecoveryOTP(String email){
        if (userRepository.findAnyByEmailIncludingDeleted(email).isEmpty()){
            throw new BadRequestException("No user found for this email: " + email);
        }
        return createAndStoreOtp(email);
    }

    public String generateTutorRecoveryOTP(String email){
        if (tutorRepository.findAnyByEmailIncludingDeleted(email).isEmpty()){
            throw new BadRequestException("No tutor found for this email: " + email);
        }
        return createAndStoreOtp(email);
    }

    public String generateAdminRecoveryOTP(String adminEmail){
        if (adminRepository.findAnyByAdminEmailIncludingDeleted(adminEmail).isEmpty()){
            throw new BadRequestException("No admin found for this email: " + adminEmail);
        }
        return createAndStoreOtp(adminEmail);
    }

    public String refreshRegistrationOTP(String email) {
        return generateRegistrationOTP(email);
    }

    public String refreshAdminRegistrationOTP(String adminEmail){
        return generateAdminRegistrationOTP(adminEmail);
    }

    public String refreshTutorRegistrationOTP(String email){
        return generateTutorRegistrationOTP(email);
    }

    public String refreshPasswordOTP(String email) {
        return generateForgottenPasswordOTP(email);
    }

    public String refreshAdminPasswordOTP(String adminEmail){
        return generateAdminForgottenPasswordOTP(adminEmail);
    }

    public String refreshTutorPasswordOTP(String email){
        return generateTutorForgottenPasswordOTP(email);
    }

    public String refreshRecoveryOTP(String email){
        return generateRecoveryOTP(email);
    }

    public String refreshTutorRecoveryOTP(String email){
        return generateTutorRecoveryOTP(email);
    }

    public String refreshAdminRecoveryOTP(String adminEmail){
        return generateAdminRecoveryOTP(adminEmail);
    }

    public boolean validateRegistrationOTP(String otp, String email) {
        return validateOtp(otp, email);
    }

    public boolean validateAdminRegistrationOTP(String otp, String adminEmail){
        return validateOtp(otp, adminEmail);
    }

    public boolean validateTutorRegistrationOTP(String otp, String email){
        return validateOtp(otp, email);
    }

    public boolean validatePasswordOTP(String otp, String email) {
        return validateOtp(otp, email);
    }

    public boolean validateAdminPasswordOTP(String otp, String adminEmail){
        return validateOtp(otp, adminEmail);
    }

    public boolean validateTutorPasswordOTP(String otp, String email){
        return validateOtp(otp, email);
    }

    public boolean validateRecoveryOTP(String otp, String email) {return validateOtp(otp, email);}

    public boolean validateTutorRecoveryOTP(String otp, String email) {return validateOtp(otp, email);}

    public boolean validateAdminRecoveryOTP(String otp, String adminEmail) {return validateOtp(otp, adminEmail);}

    public void clearOtp(String email) {
        redisTemplate.delete(OTP_KEY + email);
        redisTemplate.delete(ATTEMPT_KEY + email);
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(otp.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash OTP", e);
        }
    }

    // Prevents Timing Attacks during OTP verification
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private String createAndStoreOtp(String email) {
        checkCooldown(email);

        // Dynamically generate OTP based on config
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        String otp = String.valueOf(new SecureRandom().nextInt(max - min + 1) + min);

        // Store hash and state in Redis
        redisTemplate.opsForValue().set(OTP_KEY + email, hashOtp(otp), Duration.ofMinutes(otpExpiryMinutes));
        redisTemplate.opsForValue().set(COOLDOWN_KEY + email, "true", Duration.ofSeconds(60));
        redisTemplate.opsForValue().set(ATTEMPT_KEY + email, "0", Duration.ofMinutes(otpExpiryMinutes));

        return otp;
    }

    private void checkCooldown(String email) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_KEY + email))) {
            throw new RuntimeException("OTP was recently sent. Please wait before refreshing.");
        }
    }

    private boolean validateOtp(String otp, String email) {
        String storedHash = redisTemplate.opsForValue().get(OTP_KEY + email);
        if (Objects.isNull(storedHash)) {
            return false;
        }

        // Checks attempts dynamically
        Long attempts = redisTemplate.opsForValue().increment(ATTEMPT_KEY + email);
        if (Objects.nonNull(attempts) && attempts > maxAttempts) {
            clearOtp(email);
            throw new RuntimeException("Too many failed attempts. Please request a new OTP.");
        }

        boolean isValid = constantTimeEquals(storedHash, hashOtp(otp));
        if (isValid) {
            clearOtp(email);
            return true;
        }
        return false;
    }
}
package com.TutorIn.service.LOGIN;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.EmailEvent;
import com.TutorIn.entity.Admin;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AdminRepository;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.OTP.OtpService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Service
public class AdminAccountRecoveryService {
    private final AdminRepository adminRepository;
    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final OtpService otpService;
    private final Logger log = LoggerFactory.getLogger(AdminAccountRecoveryService.class);

    public void initiateRecovery(String adminEmail) {
        Admin admin = adminRepository.findAnyByAdminEmailIncludingDeleted(adminEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!admin.isDeleted()) {
            throw new BadRequestException("This account is already active. Please log in normally.");
        }

        String otp = otpService.generateAdminRecoveryOTP(admin.getAdminEmail());

        String body = "Welcome back to TutorIn! Your account recovery OTP is: " + otp +
                ". It expires in 10 minutes.";

        EmailEvent emailEvent = new EmailEvent(
                adminEmail,
                "TutorIn Account Recovery",
                body,
                "ACCOUNT RECOVERY"
        );

        kafkaTemplate.send("email-topic", emailEvent);
        log.info("Recovery OTP sent to Kafka for user: {}", adminEmail);
    }

    @Transactional
    public AuthTokens confirmRecovery(String adminEmail, String otp) {
        boolean isValid = otpService.validateAdminRecoveryOTP(otp,adminEmail);
        if (!isValid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        Admin admin = adminRepository.findAnyByAdminEmailIncludingDeleted(adminEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        admin.setDeleted(false);
        admin.setActive(true);
        admin.setUpdatedAt(OffsetDateTime.now());
        adminRepository.save(admin);

        otpService.clearOtp(adminEmail);

        UserDetails userDetails = userDetailsService.loadUserByUsername(adminEmail);
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(adminEmail);

        log.info("Account successfully recovered and logged in for user: {}", adminEmail);

        return new AuthTokens(accessToken, refreshToken);
    }
}

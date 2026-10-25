package com.TutorIn.service.LOGIN;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.EmailEvent;
import com.TutorIn.entity.Tutor;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.TutorRepository;
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

@Service
@RequiredArgsConstructor
public class TutorAccountRecoveryService {
    private final TutorRepository tutorRepository;
    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final OtpService otpService;
    private final Logger log = LoggerFactory.getLogger(TutorAccountRecoveryService.class);

    public void initiateRecovery(String email) {
        Tutor tutor = tutorRepository.findAnyByEmailIncludingDeleted(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!tutor.isDeleted()) {
            throw new BadRequestException("This account is already active. Please log in normally.");
        }

        String otp = otpService.generateTutorRecoveryOTP(tutor.getEmail());

        String body = "Welcome back to TutorIn! Your account recovery OTP is: " + otp + ". It expires in 10 minutes.";

        EmailEvent emailEvent = new EmailEvent(
                email,
                "TutorIn Account Recovery",
                body,
                "ACCOUNT RECOVERY"
        );

        kafkaTemplate.send("email-topic", emailEvent);
        log.info("Recovery OTP sent to Kafka for user: {}", email);
    }

    @Transactional
    public AuthTokens confirmRecovery(String email, String otp) {
        boolean isValid = otpService.validateRecoveryOTP(otp,email);
        if (!isValid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        Tutor tutor = tutorRepository.findAnyByEmailIncludingDeleted(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        tutor.setDeleted(false);
        tutor.setActive(true);
        tutor.setUpdatedAt(OffsetDateTime.now());
        tutorRepository.save(tutor);

        otpService.clearOtp(email);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(email);

        log.info("Account successfully recovered and logged in for user: {}", email);

        return new AuthTokens(accessToken, refreshToken);
    }
}

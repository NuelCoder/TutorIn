package com.TutorIn.service.LOGIN;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.EmailEvent;
import com.TutorIn.entity.Tutor;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.OTP.OtpService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TutorAccountRecoveryServiceTest {

    @Mock private TutorRepository tutorRepository;
    @Mock private KafkaTemplate<String, EmailEvent> kafkaTemplate;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private OtpService otpService;

    @Mock private UserDetails userDetails;

    @InjectMocks
    private TutorAccountRecoveryService recoveryService;

    private Tutor mockTutor;
    private final String email = "tutor@tutorin.com";

    @BeforeEach
    public void setUp() {
        mockTutor = new Tutor();
        mockTutor.setEmail(email);
        mockTutor.setDeleted(true); // Must be true to allow recovery
    }

    @Test
    public void initiateRecovery_whenValid_thenSendsEmail() {
        when(tutorRepository.findAnyByEmailIncludingDeleted(email)).thenReturn(Optional.of(mockTutor));
        when(otpService.generateTutorRecoveryOTP(email)).thenReturn("123456");

        recoveryService.initiateRecovery(email);

        verify(kafkaTemplate, times(1)).send(eq("email-topic"), any(EmailEvent.class));
    }

    @Test
    public void initiateRecovery_whenAccountNotDeleted_thenThrowsBadRequest() {
        mockTutor.setDeleted(false); // Account is already active
        when(tutorRepository.findAnyByEmailIncludingDeleted(email)).thenReturn(Optional.of(mockTutor));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                recoveryService.initiateRecovery(email));

        assertEquals("This account is already active. Please log in normally.", exception.getMessage());
    }

    @Test
    public void confirmRecovery_whenOtpValid_thenRecoversAndReturnsTokens() {
        when(otpService.validateTutorRecoveryOTP("123456", email)).thenReturn(true);
        when(tutorRepository.findAnyByEmailIncludingDeleted(email)).thenReturn(Optional.of(mockTutor));

        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(email)).thenReturn("refresh-token");

        AuthTokens tokens = recoveryService.confirmRecovery(email, "123456");

        assertFalse(mockTutor.isDeleted());
        assertTrue(mockTutor.isActive());
        verify(tutorRepository, times(1)).save(mockTutor);
        verify(otpService, times(1)).clearOtp(email);
        assertEquals("access-token", tokens.accessToken());
    }

    @Test
    public void confirmRecovery_whenOtpInvalid_thenThrowsBadRequest() {
        when(otpService.validateTutorRecoveryOTP("wrong-otp", email)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                recoveryService.confirmRecovery(email, "wrong-otp"));

        assertEquals("Invalid or expired OTP", exception.getMessage());
        verify(tutorRepository, never()).save(any());
    }
}
package com.TutorIn.service.LOGIN;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.EmailEvent;
import com.TutorIn.entity.User;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.UserRepository;
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
public class UserAccountRecoveryServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private KafkaTemplate<String, EmailEvent> kafkaTemplate;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private OtpService otpService;

    @Mock private UserDetails userDetails;

    @InjectMocks
    private UserAccountRecoveryService recoveryService;

    private User mockUser;
    private final String email = "user@tutorin.com";

    @BeforeEach
    public void setUp() {
        mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setDeleted(true); // Must be true to allow recovery
    }

    @Test
    public void initiateRecovery_whenValid_thenSendsEmail() {
        when(userRepository.findAnyByEmailIncludingDeleted(email)).thenReturn(Optional.of(mockUser));
        when(otpService.generateRecoveryOTP(email)).thenReturn("123456");

        recoveryService.initiateRecovery(email);

        verify(kafkaTemplate, times(1)).send(eq("email-topic"), any(EmailEvent.class));
    }

    @Test
    public void initiateRecovery_whenAccountNotDeleted_thenThrowsBadRequest() {
        mockUser.setDeleted(false); // Account is already active
        when(userRepository.findAnyByEmailIncludingDeleted(email)).thenReturn(Optional.of(mockUser));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                recoveryService.initiateRecovery(email));

        assertEquals("This account is already active. Please log in normally.", exception.getMessage());
    }

    @Test
    public void confirmRecovery_whenOtpValid_thenRecoversAndReturnsTokens() {
        when(otpService.validateRecoveryOTP("123456", email)).thenReturn(true);
        when(userRepository.findAnyByEmailIncludingDeleted(email)).thenReturn(Optional.of(mockUser));

        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(email)).thenReturn("refresh-token");

        AuthTokens tokens = recoveryService.confirmRecovery(email, "123456");

        assertFalse(mockUser.isDeleted());
        assertTrue(mockUser.isActive());
        verify(userRepository, times(1)).save(mockUser);
        verify(otpService, times(1)).clearOtp(email);
        assertEquals("access-token", tokens.accessToken());
    }

    @Test
    public void confirmRecovery_whenOtpInvalid_thenThrowsBadRequest() {
        when(otpService.validateRecoveryOTP("wrong-otp", email)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                recoveryService.confirmRecovery(email, "wrong-otp"));

        assertEquals("Invalid or expired OTP", exception.getMessage());
        verify(userRepository, never()).save(any());
    }
}
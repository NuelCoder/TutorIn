package com.TutorIn.service.LOGIN;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.EmailEvent;
import com.TutorIn.entity.Admin;
import com.TutorIn.entity.User;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AdminRepository;
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
public class AdminAccountRecoveryServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private KafkaTemplate<String, EmailEvent> kafkaTemplate;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private OtpService otpService;

    @Mock private UserDetails userDetails;

    @InjectMocks
    private AdminAccountRecoveryService recoveryService;

    private Admin mockAdmin;
    private final String email = "admin@tutorin.com";

    @BeforeEach
    public void setUp() {
        mockAdmin = new Admin();
        mockAdmin.setAdminEmail(email);
        mockAdmin.setDeleted(true); // Must be true to allow recovery
    }

    @Test
    public void initiateRecovery_whenValid_thenSendsEmail() {
        when(adminRepository.findAnyByAdminEmailIncludingDeleted(email)).thenReturn(Optional.of(mockAdmin));
        when(otpService.generateAdminRecoveryOTP(email)).thenReturn("123456");

        recoveryService.initiateRecovery(email);

        verify(kafkaTemplate, times(1)).send(eq("email-topic"), any(EmailEvent.class));
    }

    @Test
    public void initiateRecovery_whenAccountNotDeleted_thenThrowsBadRequest() {
        mockAdmin.setDeleted(false); // Account is already active
        when(adminRepository.findAnyByAdminEmailIncludingDeleted(email)).thenReturn(Optional.of(mockAdmin));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                recoveryService.initiateRecovery(email));

        assertEquals("This account is already active. Please log in normally.", exception.getMessage());
    }

    @Test
    public void confirmRecovery_whenOtpValid_thenRecoversAndReturnsTokens() {
        when(otpService.validateAdminRecoveryOTP("123456", email)).thenReturn(true);
        when(adminRepository.findAnyByAdminEmailIncludingDeleted(email)).thenReturn(Optional.of(mockAdmin));

        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(email)).thenReturn("refresh-token");

        AuthTokens tokens = recoveryService.confirmRecovery(email, "123456");

        assertFalse(mockAdmin.isDeleted());
        assertTrue(mockAdmin.isActive());
        verify(adminRepository, times(1)).save(mockAdmin);
        verify(otpService, times(1)).clearOtp(email);
        assertEquals("access-token", tokens.accessToken());
    }

    @Test
    public void confirmRecovery_whenOtpInvalid_thenThrowsBadRequest() {
        when(otpService.validateAdminRecoveryOTP("wrong-otp", email)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                recoveryService.confirmRecovery(email, "wrong-otp"));

        assertEquals("Invalid or expired OTP", exception.getMessage());
        verify(adminRepository, never()).save(any());
    }
}
package com.TutorIn.service.OTP;

import com.TutorIn.entity.PendingUser;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private PendingUserRepository pendingUserRepository;

    // (Other repositories omitted for brevity, logic is identical)

    @InjectMocks
    private OtpService otpService;

    private final String email = "user@tutorin.com";

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 10);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
    }

    @Test
    public void generateRegistrationOTP_whenUserExists_thenStoresAndReturnsOtp() {
        when(pendingUserRepository.findByEmail(email)).thenReturn(Optional.of(new PendingUser()));
        when(redisTemplate.hasKey("otp_cooldown:" + email)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String otp = otpService.generateRegistrationOTP(email);

        assertNotNull(otp);
        assertEquals(6, otp.length()); // Ensures the dynamic length works
        verify(valueOperations, times(3)).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    public void generateRegistrationOTP_whenUserNotFound_thenThrowsException() {
        when(pendingUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> otpService.generateRegistrationOTP(email));
    }

    @Test
    public void generateRegistrationOTP_whenOnCooldown_thenThrowsException() {
        when(pendingUserRepository.findByEmail(email)).thenReturn(Optional.of(new PendingUser()));
        when(redisTemplate.hasKey("otp_cooldown:" + email)).thenReturn(true); // Cooldown active

        RuntimeException exception = assertThrows(RuntimeException.class, () -> otpService.generateRegistrationOTP(email));
        assertEquals("OTP was recently sent. Please wait before refreshing.", exception.getMessage());
    }
}
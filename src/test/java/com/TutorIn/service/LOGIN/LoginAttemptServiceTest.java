package com.TutorIn.service.LOGIN;

import com.TutorIn.exceptions.BadRequestException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginAttemptServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    private final String email = "user@tutorin.com";
    private final String cacheKey = "login_attempts:user@tutorin.com";

    @BeforeEach
    public void setUp() {
        // Manually inject the @Value property using Reflection since we aren't using @SpringBootTest
        ReflectionTestUtils.setField(loginAttemptService, "security_max_login_attempts", 5);
    }

    @Test
    public void checkLoginAllowed_whenAttemptsExceedLimit_thenThrowsException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("5");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                loginAttemptService.checkLoginAllowed(email));

        assertEquals("Account is temporarily locked due to too many failed login attempts. Please try again in 15 minutes.", exception.getMessage());
    }

    @Test
    public void registerFailedLogin_whenFirstAttempt_thenSetsExpiration() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(cacheKey)).thenReturn(1L);

        loginAttemptService.registerFailedLogin(email);

        verify(redisTemplate, times(1)).expire(eq(cacheKey), any(Duration.class));
    }

    @Test
    public void registerSuccessfulLogin_thenDeletesCacheKey() {
        loginAttemptService.registerSuccessfulLogin(email);

        verify(redisTemplate, times(1)).delete(cacheKey);
    }
}
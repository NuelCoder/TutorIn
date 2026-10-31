package com.TutorIn.service.LOGOUT;

import com.TutorIn.security.JwtUtil;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LogoutServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private LogoutService logoutService;

    @Test
    public void logout_whenValidToken_thenBlacklistsInRedis() {
        String token = "mock-jwt-token";
        DefaultClaims claims = new DefaultClaims();
        // Set expiration 10 minutes into the future
        claims.setExpiration(new Date(System.currentTimeMillis() + 600000));

        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        logoutService.logout(token);

        verify(valueOperations, times(1)).set(
                eq("jwt_blacklist:" + token),
                eq("blacklisted"),
                any(Duration.class)
        );
    }

    @Test
    public void isTokenBlacklisted_whenExists_thenReturnsTrue() {
        String token = "mock-jwt-token";
        when(redisTemplate.hasKey("jwt_blacklist:" + token)).thenReturn(true);

        boolean result = logoutService.isTokenBlacklisted(token);

        assertTrue(result);
    }
}
package com.TutorIn.service.REFRESHTOKEN;

import com.TutorIn.entity.User;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AdminRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.repository.UserRepository;
import com.TutorIn.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private UserRepository userRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private JwtUtil jwtUtil;

    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private SetOperations<String, String> setOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private final String email = "user@tutorin.com";

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "tokenExpiryDays", 7L);
    }

    @Test
    public void createRefreshToken_thenStoresInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        String token = refreshTokenService.createRefreshToken(email);

        assertNotNull(token);
        verify(valueOperations, times(1)).set(eq("refresh_token:" + token), eq(email), any(Duration.class));
        verify(setOperations, times(1)).add(eq("user_sessions:" + email), eq(token));
    }

    @Test
    public void revokeAllSessions_whenTokensExist_thenDeletesAll() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("user_sessions:" + email)).thenReturn(Set.of("token1", "token2"));

        refreshTokenService.revokeAllSessions(email);

        verify(redisTemplate, times(1)).delete("refresh_token:token1");
        verify(redisTemplate, times(1)).delete("refresh_token:token2");
        verify(redisTemplate, times(1)).delete("user_sessions:" + email);
    }

    @Test
    public void processRefreshToken_whenUserValid_thenReturnNewJwt() {
        String token = "valid-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:" + token)).thenReturn(email);

        User mockUser = new User();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken(mockUser)).thenReturn("new-jwt-token");

        String newJwt = refreshTokenService.processRefreshToken(token, Role.USER);

        assertEquals("new-jwt-token", newJwt);
    }

    @Test
    public void processRefreshToken_whenNullToken_thenThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> refreshTokenService.processRefreshToken(null, Role.USER));
    }
}
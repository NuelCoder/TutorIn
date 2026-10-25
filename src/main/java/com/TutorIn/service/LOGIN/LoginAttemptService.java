package com.TutorIn.service.LOGIN;

import com.TutorIn.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final StringRedisTemplate redisTemplate;

    private static final String ATTEMPTS_CACHE_KEY = "login_attempts:";
    // We will lock them out for 15 minutes if they exceed the limit
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    @Value("${security_max_login_attempts:5}")
    private int security_max_login_attempts;

    /**
     * Checks if the user has exceeded the max login attempts allowed.
     */
    public void checkLoginAllowed(String email) {
        String attemptsStr = redisTemplate.opsForValue().get(ATTEMPTS_CACHE_KEY + email);
        if (attemptsStr == null) return; // No failed attempts yet

        int attempts = Integer.parseInt(attemptsStr);
        int maxAttempts = security_max_login_attempts;

        if (attempts >= maxAttempts) {
            throw new BadRequestException("Account is temporarily locked due to too many failed login attempts. Please try again in 15 minutes.");
        }
    }

    /**
     * Increments the failed attempt counter for the user.
     */
    public void registerFailedLogin(String email) {
        String key = ATTEMPTS_CACHE_KEY + email;
        Long attempts = redisTemplate.opsForValue().increment(key);

        // If this is their first failed attempt, set the 15-minute countdown clock
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
        }
    }

    /**
     * Clears the failed attempt counter when they log in successfully.
     */
    public void registerSuccessfulLogin(String email) {
        redisTemplate.delete(ATTEMPTS_CACHE_KEY + email);
    }
}
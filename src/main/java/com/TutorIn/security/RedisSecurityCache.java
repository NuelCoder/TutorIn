package com.TutorIn.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisSecurityCache {
    private final StringRedisTemplate redisTemplate;

    private static final String SUSPENDED_PREFIX = "suspended_user:";
    private static final String CUTOFF_PREFIX = "token_cutoff:";

    // We set a Time-To-Live (TTL) that covers the maximum lifespan of an access token.
    // Adjust this to match whatever expiration time you set in your JwtUtil.
    private static final Duration BLOCK_DURATION = Duration.ofDays(1);

    public void blockUser(String email) {
        // Drops the kill-switch flag into Redis
        redisTemplate.opsForValue().set(SUSPENDED_PREFIX + email, "true", BLOCK_DURATION);
    }

    public void unblockUser(String email) {
        // Lifts the kill-switch
        redisTemplate.delete(SUSPENDED_PREFIX + email);
    }

    public boolean isUserSuspended(String email) {
        // Blazing fast check to see if the key exists
        return redisTemplate.hasKey(SUSPENDED_PREFIX + email);
    }

    public void setTokenCutoff(String email) {
        // Record the exact millisecond this method is called
        String currentTime = String.valueOf(System.currentTimeMillis());
        redisTemplate.opsForValue().set(CUTOFF_PREFIX + email, currentTime, Duration.ofDays(7));
    }

    public Long getTokenCutoffTime(String email) {
        String timeStr = redisTemplate.opsForValue().get(CUTOFF_PREFIX + email);
        return timeStr != null ? Long.parseLong(timeStr) : null;
    }

}

package com.TutorIn.service.LOGOUT;

import com.TutorIn.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String BLACKLIST_PREFIX = "jwt_blacklist:";

    public void logout(String jwt) {
        // Extract the expiration date directly from the token
        Date expirationDate = jwtUtil.extractAllClaims(jwt).getExpiration();
        long remainingMillis = expirationDate.getTime() - System.currentTimeMillis();

        if (remainingMillis > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jwt,
                    "blacklisted",
                    Duration.ofMillis(remainingMillis)
            );
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
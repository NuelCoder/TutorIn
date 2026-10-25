package com.TutorIn.service.REFRESHTOKEN;

import com.TutorIn.entity.Admin;
import com.TutorIn.entity.Tutor;
import com.TutorIn.entity.User;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AdminRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.repository.UserRepository;
import com.TutorIn.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TutorRepository tutorRepository;
    private final JwtUtil jwtUtil;

    @Value("${token.expiry-days:7}")
    private long tokenExpiryDays;

    private static final String REFRESH_PREFIX = "refresh_token:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";

    public String createRefreshToken(String email) {
        String token = UUID.randomUUID().toString();
        String tokenKey = REFRESH_PREFIX + token;
        String userKey = USER_SESSIONS_PREFIX + email;

        Duration duration = Duration.ofDays(tokenExpiryDays);

        // No manual expiry checking needed; Redis deletes it after Duration
        redisTemplate.opsForValue().set(
                tokenKey,
                email,
                duration
        );
        redisTemplate.opsForSet().add(userKey, token);
        redisTemplate.expire(userKey, duration);
        return token;
    }

    public String validateAndGetEmail(String token) {
        String email = redisTemplate.opsForValue().get(REFRESH_PREFIX + token);

        if (email == null) {
            throw new RuntimeException("Refresh token is invalid or has expired.");
        }

        return email;
    }

    /**
     * Use this for standard Logout (single device)
     */
    public void revokeSingleToken(String token) {
        String email = redisTemplate.opsForValue().get(REFRESH_PREFIX + token);
        if (email != null) {
            redisTemplate.delete(REFRESH_PREFIX + token);
            redisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + email, token);
        }
    }

    public void revokeAllSessions(String email) {
        String userKey = USER_SESSIONS_PREFIX + email;

        // 1. Get all token UUIDs associated with this email
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);

        if (tokens != null) {
            // 2. Delete each individual token from Redis
            for (String token : tokens) {
                redisTemplate.delete(REFRESH_PREFIX + token);
            }
        }

        // 3. Clear the user's session list
        redisTemplate.delete(userKey);
    }

    public String processRefreshToken(String incomingRefreshToken, Role requiredRole) throws BadRequestException {
        if (incomingRefreshToken == null) {
            throw new BadRequestException("No refresh token found. Please log in.");
        }

        String email = validateAndGetEmail(incomingRefreshToken);

        if (requiredRole == Role.ADMIN) {
            return processAdminRefresh(email);
        } else if (requiredRole == Role.TUTOR) {
            return processTutorRefresh(email);
        }else{
            return processUserRefresh(email);
        }
    }

    // --- HELPER METHODS ---

    private String processUserRefresh(String email) throws BadRequestException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    revokeAllSessions(email);
                    return new BadRequestException("Action rejected: User account not found.");
                });

        userRepository.save(user);

        return jwtUtil.generateToken(user);
    }

    private String processAdminRefresh(String email) throws BadRequestException {
        Admin admin = adminRepository.findByAdminEmail(email)
                .orElseThrow(() -> {
                    revokeAllSessions(email);
                    return new BadRequestException("Action rejected: Admin account not found.");
                });

        return jwtUtil.generateToken(admin);
    }

    private String processTutorRefresh(String email) throws BadRequestException {
        Tutor tutor = tutorRepository.findByEmail(email)
                .orElseThrow(() -> {
                    revokeAllSessions(email);
                    return new BadRequestException("Action rejected: Admin account not found.");
                });

        return jwtUtil.generateToken(tutor);
    }
}

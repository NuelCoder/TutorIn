package com.TutorIn.security;

import com.TutorIn.service.LOGOUT.LogoutService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final LogoutService logoutService;
    private final RedisSecurityCache redisSecurityCache;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Check for the Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username;
        Date issuedAt;

        // 2. Catch Malformed or Expired Tokens immediately
        try {
            username = jwtUtil.extractUserName(jwt);
            // 3. Extract the issue date to compare against the kill-switch
            issuedAt = jwtUtil.extractIssuedAt(jwt);
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Invalid or expired token. Please log in again.");
            return;
        }

        // 4. The Redis Strike: Block blacklisted tokens instantly -> 401
        if (logoutService.isTokenBlacklisted(jwt)) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Token is blacklisted. Please log in again.");
            return;
        }

        // 5. The Redis Kill-Switch: Block suspended users or revoked sessions -> 401/403
        if (username != null) {
            if (redisSecurityCache.isUserSuspended(username)) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Account has been suspended. Access revoked.");
                return;
            }

            Long cutoffTime = redisSecurityCache.getTokenCutoffTime(username);
            if (cutoffTime != null && issuedAt.getTime() < cutoffTime) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Session expired due to account security changes. Please log in again.");
                return;
            }
        }

        // 6. Normal Authentication flow
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!userDetails.isAccountNonLocked() || !userDetails.isEnabled()) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Account has been suspended. Access revoked.");
                return;
            }

            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Token is invalid.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // Standardized to match the exact JSON format of the GlobalExceptionHandler
    private void sendErrorResponse(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format("{\"error\": {\"code\": \"%s\", \"message\": \"%s\"}}", code, message);
        response.getWriter().write(jsonResponse);
    }
}
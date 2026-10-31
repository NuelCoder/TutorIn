package com.TutorIn.service.LOGIN;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.LoginRequest;
import com.TutorIn.entity.User;
import com.TutorIn.exceptions.AccountPendingRecoveryException;
import com.TutorIn.exceptions.AccountPermanentlyDeletedException;
import com.TutorIn.exceptions.BadCredentialsException;
import com.TutorIn.repository.UserRepository;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserLoginServiceTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;
    @Mock private LoginAttemptService loginAttemptService;

    @Mock private Authentication authentication;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private UserLoginService userLoginService;

    private User mockUser;
    private LoginRequest request;

    @BeforeEach
    public void setUp() {
        mockUser = new User();
        mockUser.setEmail("user@tutorin.com");
        mockUser.setSuspended(false);
        mockUser.setDeleted(false);

        request = new LoginRequest("user@tutorin.com", "password");
    }

    @Test
    public void loginUser_whenValid_thenReturnTokens() {
        when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(request.getUsername());
        when(jwtUtil.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(request.getUsername())).thenReturn("refresh-token");

        AuthTokens tokens = userLoginService.loginUser(request);

        assertNotNull(tokens);
        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
        verify(loginAttemptService, times(1)).registerSuccessfulLogin(request.getUsername());
    }

    @Test
    public void loginUser_whenUserSuspended_thenThrowsDisabledException() {
        mockUser.setSuspended(true);
        when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(mockUser));

        assertThrows(DisabledException.class, () -> userLoginService.loginUser(request));
    }

    @Test
    public void loginUser_whenPendingRecovery_thenThrowsAccountPendingRecoveryException() {
        mockUser.setDeleted(true);
        mockUser.setUpdatedAt(OffsetDateTime.now().minusDays(10)); // Deleted recently
        when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(mockUser));

        assertThrows(AccountPendingRecoveryException.class, () -> userLoginService.loginUser(request));
    }

    @Test
    public void loginUser_whenPermanentlyDeleted_thenThrowsAccountPermanentlyDeletedException() {
        mockUser.setDeleted(true);
        mockUser.setUpdatedAt(OffsetDateTime.now().minusDays(31)); // Deleted > 30 days ago
        when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(mockUser));

        assertThrows(AccountPermanentlyDeletedException.class, () -> userLoginService.loginUser(request));
    }
}
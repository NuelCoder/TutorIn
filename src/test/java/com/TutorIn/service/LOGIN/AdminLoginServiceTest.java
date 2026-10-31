package com.TutorIn.service.LOGIN;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.LoginRequest;
import com.TutorIn.entity.Admin;
import com.TutorIn.entity.Tutor;
import com.TutorIn.exceptions.AccountPendingRecoveryException;
import com.TutorIn.exceptions.AccountPermanentlyDeletedException;
import com.TutorIn.repository.AdminRepository;
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
public class AdminLoginServiceTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AdminRepository adminRepository;
    @Mock private LoginAttemptService loginAttemptService;

    @Mock private Authentication authentication;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private AdminLoginService adminLoginService;

    private Admin mockAdmin;
    private LoginRequest request;

    @BeforeEach
    public void setUp() {
        mockAdmin = new Admin();
        mockAdmin.setAdminEmail("admin@tutorin.com");
        mockAdmin.setDeleted(false);

        request = new LoginRequest("admin@tutorin.com", "password");
    }

    @Test
    public void loginAdmin_whenValid_thenReturnTokens() {
        when(adminRepository.findByAdminEmail(request.getUsername())).thenReturn(Optional.of(mockAdmin));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(request.getUsername());
        when(jwtUtil.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(request.getUsername())).thenReturn("refresh-token");

        AuthTokens tokens = adminLoginService.loginAdmin(request);

        assertNotNull(tokens);
        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
        verify(loginAttemptService, times(1)).registerSuccessfulLogin(request.getUsername());
    }

    @Test
    public void loginAdmin_whenPendingRecovery_thenThrowsAccountPendingRecoveryException() {
        mockAdmin.setDeleted(true);
        mockAdmin.setUpdatedAt(OffsetDateTime.now().minusDays(10)); // Deleted recently
        when(adminRepository.findByAdminEmail(request.getUsername())).thenReturn(Optional.of(mockAdmin));

        assertThrows(AccountPendingRecoveryException.class, () -> adminLoginService.loginAdmin(request));
    }

    @Test
    public void loginAdmin_whenPermanentlyDeleted_thenThrowsAccountPermanentlyDeletedException() {
        mockAdmin.setDeleted(true);
        mockAdmin.setUpdatedAt(OffsetDateTime.now().minusDays(31)); // Deleted > 30 days ago
        when(adminRepository.findByAdminEmail(request.getUsername())).thenReturn(Optional.of(mockAdmin));

        assertThrows(AccountPermanentlyDeletedException.class, () -> adminLoginService.loginAdmin(request));
    }
}
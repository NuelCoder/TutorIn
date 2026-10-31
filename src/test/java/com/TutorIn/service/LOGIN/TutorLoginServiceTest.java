package com.TutorIn.service.LOGIN;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.LoginRequest;
import com.TutorIn.entity.Tutor;
import com.TutorIn.exceptions.AccountPendingRecoveryException;
import com.TutorIn.exceptions.AccountPermanentlyDeletedException;
import com.TutorIn.repository.TutorRepository;
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
public class TutorLoginServiceTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private TutorRepository tutorRepository;
    @Mock private LoginAttemptService loginAttemptService;

    @Mock private Authentication authentication;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private TutorLoginService tutorLoginService;

    private Tutor mockTutor;
    private LoginRequest request;

    @BeforeEach
    public void setUp() {
        mockTutor = new Tutor();
        mockTutor.setEmail("tutor@tutorin.com");
        mockTutor.setSuspended(false);
        mockTutor.setDeleted(false);

        request = new LoginRequest("tutor@tutorin.com", "password");
    }

    @Test
    public void loginTutor_whenValid_thenReturnTokens() {
        when(tutorRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(mockTutor));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(request.getUsername());
        when(jwtUtil.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(request.getUsername())).thenReturn("refresh-token");

        AuthTokens tokens = tutorLoginService.loginTutor(request);

        assertNotNull(tokens);
        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
        verify(loginAttemptService, times(1)).registerSuccessfulLogin(request.getUsername());
    }

    @Test
    public void loginTutor_whenTutorSuspended_thenThrowsDisabledException() {
        mockTutor.setSuspended(true);
        when(tutorRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(mockTutor));

        assertThrows(DisabledException.class, () -> tutorLoginService.loginTutor(request));
    }

    @Test
    public void loginTutor_whenPendingRecovery_thenThrowsAccountPendingRecoveryException() {
        mockTutor.setDeleted(true);
        mockTutor.setUpdatedAt(OffsetDateTime.now().minusDays(10)); // Deleted recently
        when(tutorRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(mockTutor));

        assertThrows(AccountPendingRecoveryException.class, () -> tutorLoginService.loginTutor(request));
    }

    @Test
    public void loginTutor_whenPermanentlyDeleted_thenThrowsAccountPermanentlyDeletedException() {
        mockTutor.setDeleted(true);
        mockTutor.setUpdatedAt(OffsetDateTime.now().minusDays(31)); // Deleted > 30 days ago
        when(tutorRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(mockTutor));

        assertThrows(AccountPermanentlyDeletedException.class, () -> tutorLoginService.loginTutor(request));
    }
}
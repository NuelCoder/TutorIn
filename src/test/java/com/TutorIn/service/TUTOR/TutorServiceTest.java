package com.TutorIn.service.TUTOR;

import com.TutorIn.dto.EmailEvent;
import com.TutorIn.dto.OtpVerificationRequest;
import com.TutorIn.dto.UpdateTutor;
import com.TutorIn.entity.PendingTutor;
import com.TutorIn.entity.Tutor;
import com.TutorIn.exceptions.AccessDeniedException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.PendingTutorRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.service.OTP.OtpService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TutorServiceTest {

    @Mock private TutorRepository tutorRepository;
    @Mock private PendingTutorRepository pendingTutorRepository;
    @Mock private OtpService otpService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private KafkaTemplate<String, EmailEvent> kafkaTemplate;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private TutorService tutorService;

    private final String email = "tutor@tutorin.com";

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String loggedInEmail, String role) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(loggedInEmail);

        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        doReturn(authorities).when(authentication).getAuthorities();

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void verifyOtpAndRegisterUser_whenValid_thenSavesTutor() {
        OtpVerificationRequest request = new OtpVerificationRequest(email, "123456");
        PendingTutor pendingTutor = new PendingTutor();
        pendingTutor.setEmail(email);

        when(otpService.validateTutorRegistrationOTP("123456", email)).thenReturn(true);
        when(pendingTutorRepository.findByEmail(email)).thenReturn(Optional.of(pendingTutor));

        tutorService.verifyOtpAndRegisterUser(request);

        verify(tutorRepository, times(1)).save(any(Tutor.class));
        verify(kafkaTemplate, times(1)).send(eq("email-topic"), any(EmailEvent.class));
        verify(pendingTutorRepository, times(1)).delete(pendingTutor);
        verify(otpService, times(1)).clearOtp(email);
    }

    @Test
    public void getTutorProfile_whenEmailsMatch_thenReturnTutor() {
        mockSecurityContext(email, "ROLE_TUTOR");
        Tutor mockTutor = new Tutor();
        mockTutor.setEmail(email);

        when(tutorRepository.findByEmail(email)).thenReturn(Optional.of(mockTutor));

        Tutor result = tutorService.getTutorProfile(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    public void deleteTutorProfile_whenValid_thenMarksDeletedAndRevokesTokens() {
        mockSecurityContext(email, "ROLE_TUTOR");
        Tutor mockTutor = new Tutor();
        mockTutor.setEmail(email);
        mockTutor.setActive(true);

        when(tutorRepository.findByEmail(email)).thenReturn(Optional.of(mockTutor));

        tutorService.deleteTutorProfile(email);

        assertFalse(mockTutor.isActive());
        assertTrue(mockTutor.isDeleted());
        verify(tutorRepository, times(1)).save(mockTutor);
        verify(refreshTokenService, times(1)).revokeAllSessions(email);
    }

    @Test
    public void updateTutorProfile_whenTryingToUpdateEmailAsTutor_thenThrowsAccessDenied() {
        mockSecurityContext(email, "ROLE_TUTOR");
        UpdateTutor updateRequest = new UpdateTutor("Name", "Male", "Bio", "new-email@tutorin.com", "Country");

        Tutor mockTutor = new Tutor();
        mockTutor.setEmail(email);

        when(tutorRepository.findByEmail(email)).thenReturn(Optional.of(mockTutor));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                tutorService.updateTutorProfile(email, updateRequest));

        assertTrue(exception.getMessage().contains("You are not allowed to update these fields directly"));
    }

    @Test
    public void initiateForgotPassword_whenValid_thenSendsEmail() {
        Tutor mockTutor = new Tutor();
        mockTutor.setEmail(email);

        when(tutorRepository.findByEmail(email)).thenReturn(Optional.of(mockTutor));
        when(otpService.generateTutorForgottenPasswordOTP(email)).thenReturn("123456");

        tutorService.initiateForgotPassword(email);

        verify(kafkaTemplate, times(1)).send(eq("email-topic"), any(EmailEvent.class));
    }
}
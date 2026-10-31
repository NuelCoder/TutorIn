package com.TutorIn.service.USER;

import com.TutorIn.dto.EmailEvent;
import com.TutorIn.dto.OtpVerificationRequest;
import com.TutorIn.dto.UpdateUser;
import com.TutorIn.entity.PendingUser;
import com.TutorIn.entity.User;
import com.TutorIn.exceptions.AccessDeniedException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.PendingUserRepository;
import com.TutorIn.repository.UserRepository;
import com.TutorIn.service.OTP.OtpService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
public class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PendingUserRepository pendingUserRepository;
    @Mock private OtpService otpService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private KafkaTemplate<String, EmailEvent> kafkaTemplate;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private final String email = "user@tutorin.com";

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
    public void verifyOtpAndRegisterUser_whenValid_thenSavesUser() {
        OtpVerificationRequest request = new OtpVerificationRequest(email, "123456");
        PendingUser pendingUser = new PendingUser();
        pendingUser.setEmail(email);

        when(otpService.validateRegistrationOTP("123456", email)).thenReturn(true);
        when(pendingUserRepository.findByEmail(email)).thenReturn(Optional.of(pendingUser));

        userService.verifyOtpAndRegisterUser(request);

        verify(userRepository, times(1)).save(any(User.class));
        verify(kafkaTemplate, times(1)).send(eq("email-topic"), any(EmailEvent.class));
        verify(pendingUserRepository, times(1)).delete(pendingUser);
        verify(otpService, times(1)).clearOtp(email);
    }

    @Test
    public void getUserProfile_whenEmailsMatch_thenReturnUser() {
        mockSecurityContext(email, "ROLE_USER");
        User mockUser = new User();
        mockUser.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        User result = userService.getUserProfile(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    public void getUserProfile_whenAdminRequests_thenReturnUser() {
        mockSecurityContext("admin@tutorin.com", "ROLE_ADMIN");
        User mockUser = new User();
        mockUser.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        User result = userService.getUserProfile(email);

        assertNotNull(result);
    }

    @Test
    public void getUserProfile_whenEmailsDoNotMatch_thenThrowsAccessDenied() {
        mockSecurityContext("hacker@tutorin.com", "ROLE_USER");

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                userService.getUserProfile(email));

        assertEquals("You can only view your own account", exception.getMessage());
    }

    @Test
    public void updateUserProfile_whenValid_thenSavesUpdatedData() {
        mockSecurityContext(email, "ROLE_USER");
        UpdateUser updateRequest = new UpdateUser("Updated Name", null, null, null);

        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setName("Old Name");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUserProfile(email, updateRequest);

        assertEquals("Updated Name", result.getName());
        verify(userRepository, times(1)).save(mockUser);
    }
}
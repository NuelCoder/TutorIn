package com.TutorIn.service.ADMIN;

import com.TutorIn.dto.OtpVerificationRequest;
import com.TutorIn.dto.UpdateAdmin;
import com.TutorIn.entity.Admin;
import com.TutorIn.entity.PendingAdmin;
import com.TutorIn.entity.User;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AdminRepository;
import com.TutorIn.repository.PendingAdminRepository;
import com.TutorIn.repository.SystemAuditLogsRepository;
import com.TutorIn.repository.UserRepository;
import com.TutorIn.security.RedisSecurityCache;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private PendingAdminRepository pendingAdminRepository;
    @Mock private UserRepository userRepository;
    @Mock private SystemAuditLogsRepository systemAuditLogRepository;
    @Mock private RedisSecurityCache redisSecurityCache;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpService otpService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    // These are required to fake a logged-in user in the Service layer
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private AdminService adminService; // Injects the mocks above directly into this real instance

    @BeforeEach
    public void setUp() {
        // We set up the faked security context, but we only apply it to methods that need it
    }

    @AfterEach
    public void tearDown() {
        // Clear the context after each test so it doesn't leak into tests that don't need it
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String email) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void verifyOtpAndRegisterUser_whenValid_thenSavesAdmin() {
        // Arrange
        OtpVerificationRequest request = new OtpVerificationRequest("admin@tutorin.com", "123456");

        PendingAdmin pendingAdmin = new PendingAdmin();
        pendingAdmin.setAdminName("Jane Doe");
        pendingAdmin.setAdminEmail("admin@tutorin.com");
        pendingAdmin.setAdminPassword("hashed-password");

        when(adminRepository.count()).thenReturn(2L);
        when(otpService.validateAdminRegistrationOTP("123456", "admin@tutorin.com")).thenReturn(true);
        when(pendingAdminRepository.findByAdminEmail("admin@tutorin.com")).thenReturn(Optional.of(pendingAdmin));

        // Act
        adminService.verifyOtpAndRegisterUser(request);

        // Assert
        verify(adminRepository, times(1)).save(any(Admin.class));
        verify(pendingAdminRepository, times(1)).delete(pendingAdmin);
        verify(otpService, times(1)).clearOtp("admin@tutorin.com");
    }

    @Test
    public void verifyOtpAndRegisterUser_whenAdminLimitReached_thenThrowsException() {
        OtpVerificationRequest request = new OtpVerificationRequest("admin@tutorin.com", "123456");

        // Mock the DB returning 3 current admins
        when(adminRepository.count()).thenReturn(3L);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                adminService.verifyOtpAndRegisterUser(request));

        assertEquals("Admin limit reached. System only supports maximum of 3 administrators.", exception.getMessage());
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    public void getAdminProfile_whenEmailsMatch_thenReturnAdmin() {
        String testEmail = "admin@tutorin.com";
        mockSecurityContext(testEmail);

        Admin mockAdmin = new Admin();
        mockAdmin.setAdminEmail(testEmail);
        mockAdmin.setAdminName("Jane Doe");

        when(adminRepository.findByAdminEmail(testEmail)).thenReturn(Optional.of(mockAdmin));

        Admin result = adminService.getAdminProfile(testEmail);

        assertNotNull(result);
        assertEquals("Jane Doe", result.getAdminName());
    }

    @Test
    public void getAdminProfile_whenEmailsDoNotMatch_thenThrowsAccessDenied() {
        mockSecurityContext("hacker@tutorin.com");

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                adminService.getAdminProfile("admin@tutorin.com"));

        assertEquals("You can only view your details", exception.getMessage());
        verify(adminRepository, never()).findByAdminEmail(anyString());
    }

    @Test
    public void toggleUserSuspension_whenSuspending_thenRevokesTokensAndCaches() {
        String adminEmail = "admin@tutorin.com";
        String userEmail = "student@tutorin.com";

        User mockUser = new User();
        mockUser.setEmail(userEmail);
        mockUser.setSuspended(false); // Currently active

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));

        // Act
        adminService.toggleUserSuspension(userEmail, true, adminEmail);

        // Assert
        assertTrue(mockUser.isSuspended());
        assertFalse(mockUser.isAccountNonLocked());
        verify(userRepository, times(1)).save(mockUser);
        verify(refreshTokenService, times(1)).revokeAllSessions(userEmail);
        verify(redisSecurityCache, times(1)).blockUser(userEmail);
        verify(redisSecurityCache, times(1)).setTokenCutoff(userEmail);
        verify(systemAuditLogRepository, times(1)).save(any()); // Checks if logAdminAction worked
    }

    @Test
    public void updateAdminProfile_whenValid_thenSavesUpdatedData() {
        String testEmail = "admin@tutorin.com";
        mockSecurityContext(testEmail);

        UpdateAdmin updateRequest = new UpdateAdmin("Updated Name", null); // Assuming UpdateAdmin constructor

        Admin mockAdmin = new Admin();
        mockAdmin.setAdminEmail(testEmail);
        mockAdmin.setAdminName("Old Name");

        when(adminRepository.findByAdminEmail(testEmail)).thenReturn(Optional.of(mockAdmin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin result = adminService.updateAdminProfile(testEmail, updateRequest);

        assertEquals("Updated Name", result.getAdminName());
        verify(adminRepository, times(1)).save(mockAdmin);
    }
}
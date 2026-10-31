package com.TutorIn.service.PENDINGADMIN;

import com.TutorIn.dto.EmailEvent;
import com.TutorIn.dto.PendingAdminDTO;
import com.TutorIn.entity.PendingAdmin;
import com.TutorIn.exceptions.BadInputException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AdminRepository;
import com.TutorIn.repository.PendingAdminRepository;
import com.TutorIn.service.OTP.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PendingAdminServiceTest {

    @Mock private PendingAdminRepository pendingAdminRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpService otpService;
    @Mock private KafkaTemplate<String, EmailEvent> kafkaTemplate;

    @InjectMocks
    private PendingAdminService pendingAdminService;

    private PendingAdminDTO validRequest;

    @BeforeEach
    public void setUp() {
        validRequest = new PendingAdminDTO();
        validRequest.setAdminName("Super Admin");
        validRequest.setAdminEmail("admin@tutorin.com");
        validRequest.setAdminPassword("StrongPass123!"); // Matches Regex
    }

    @Test
    public void registerAdmin_whenValid_thenSavesAndSendsEmail() {
        when(adminRepository.existsByAdminEmail(validRequest.getAdminEmail())).thenReturn(false);
        when(pendingAdminRepository.findByAdminEmail(validRequest.getAdminEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(validRequest.getAdminPassword())).thenReturn("encoded-password");

        PendingAdmin mockSavedAdmin = new PendingAdmin();
        mockSavedAdmin.setAdminEmail(validRequest.getAdminEmail());
        when(pendingAdminRepository.save(any(PendingAdmin.class))).thenReturn(mockSavedAdmin);

        when(otpService.generateAdminRegistrationOTP(validRequest.getAdminEmail())).thenReturn("123456");

        PendingAdmin result = pendingAdminService.registerAdmin(validRequest);

        assertNotNull(result);
        assertEquals(validRequest.getAdminEmail(), result.getAdminEmail());
        verify(pendingAdminRepository, times(1)).save(any(PendingAdmin.class));
        verify(kafkaTemplate, times(1)).send(eq("email-topic"), any(EmailEvent.class));
    }

    @Test
    public void registerAdmin_whenEmailExistsInMainRepo_thenThrowsBadRequest() {
        // According to your service logic, it checks Pending repo first, then Main repo
        when(pendingAdminRepository.findByAdminEmail(validRequest.getAdminEmail())).thenReturn(Optional.empty());
        when(adminRepository.existsByAdminEmail(validRequest.getAdminEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> pendingAdminService.registerAdmin(validRequest));
        verify(pendingAdminRepository, never()).save(any());
    }

    @Test
    public void registerAdmin_whenEmailExistsInPendingRepo_thenThrowsBadRequest() {
        when(pendingAdminRepository.findByAdminEmail(validRequest.getAdminEmail()))
                .thenReturn(Optional.of(new PendingAdmin())); // Mocking that it exists

        assertThrows(BadRequestException.class, () -> pendingAdminService.registerAdmin(validRequest));
    }

    @Test
    public void registerAdmin_whenWeakPassword_thenThrowsBadInputException() {
        when(adminRepository.existsByAdminEmail(validRequest.getAdminEmail())).thenReturn(false);
        when(pendingAdminRepository.findByAdminEmail(validRequest.getAdminEmail())).thenReturn(Optional.empty());

        validRequest.setAdminPassword("weak"); // Fails Regex

        BadInputException exception = assertThrows(BadInputException.class, () ->
                pendingAdminService.registerAdmin(validRequest));

        assertEquals("Invalid Password", exception.getMessage());
    }
}
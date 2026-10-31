package com.TutorIn.service.PENDINGUSER;

import com.TutorIn.dto.EmailEvent;
import com.TutorIn.dto.PendingUserDTO;
import com.TutorIn.entity.PendingUser;
import com.TutorIn.exceptions.BadInputException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.PendingUserRepository;
import com.TutorIn.repository.UserRepository;
import com.TutorIn.service.OTP.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PendingUserServiceTest {

    @Mock private PendingUserRepository pendingUserRepository;
    @Mock private UserRepository userRepository;
    @Mock private OtpService otpService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private KafkaTemplate<String, EmailEvent> kafkaTemplate;

    @InjectMocks
    private PendingUserService pendingUserService;

    private PendingUserDTO validRequest;

    @BeforeEach
    public void setUp() {
        validRequest = new PendingUserDTO();
        validRequest.setName("John Doe");
        validRequest.setEmail("john@tutorin.com");
        validRequest.setPassword("StrongPass123!"); // Matches Regex
        validRequest.setGender("Male");
        validRequest.setCountry("Nigeria");
    }

    @Test
    public void registerUser_whenValid_thenSavesAndSendsEmail() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(pendingUserRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encoded-password");

        PendingUser mockSavedUser = new PendingUser();
        mockSavedUser.setEmail(validRequest.getEmail());
        when(pendingUserRepository.save(any(PendingUser.class))).thenReturn(mockSavedUser);

        when(otpService.generateRegistrationOTP(validRequest.getEmail())).thenReturn("123456");

        PendingUser result = pendingUserService.registerUser(validRequest);

        assertNotNull(result);
        assertEquals(validRequest.getEmail(), result.getEmail());
        verify(pendingUserRepository, times(1)).save(any(PendingUser.class));
        verify(kafkaTemplate, times(1)).send(eq("email-topic"), any(EmailEvent.class));
    }

    @Test
    public void registerUser_whenEmailExistsInMainRepo_thenThrowsBadRequest() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> pendingUserService.registerUser(validRequest));
        verify(pendingUserRepository, never()).save(any());
    }

    @Test
    public void registerUser_whenEmailExistsInPendingRepo_thenThrowsBadRequest() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(pendingUserRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> pendingUserService.registerUser(validRequest));
    }

    @Test
    public void registerUser_whenWeakPassword_thenThrowsBadInputException() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(pendingUserRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);

        validRequest.setPassword("weak"); // Fails Regex

        BadInputException exception = assertThrows(BadInputException.class, () ->
                pendingUserService.registerUser(validRequest));

        assertEquals("Invalid Password", exception.getMessage());
    }
}
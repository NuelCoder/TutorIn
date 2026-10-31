package com.TutorIn.service.PENDINGTUTOR;

import com.TutorIn.dto.EmailEvent;
import com.TutorIn.dto.PendingTutorDTO;
import com.TutorIn.entity.PendingTutor;
import com.TutorIn.exceptions.BadInputException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.PendingTutorRepository;
import com.TutorIn.repository.TutorRepository;
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
public class PendingTutorServiceTest {

    @Mock private PendingTutorRepository pendingTutorRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private OtpService otpService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private KafkaTemplate<String, EmailEvent> kafkaTemplate;

    @InjectMocks
    private PendingTutorService pendingTutorService;

    private PendingTutorDTO validRequest;

    @BeforeEach
    public void setUp() {
        validRequest = new PendingTutorDTO();
        validRequest.setName("Jane Tutor");
        validRequest.setEmail("jane@tutorin.com");
        validRequest.setPassword("StrongPass123!"); // Matches Regex
        validRequest.setGender("Female"); // Matches Gender Regex
        validRequest.setCountry("Nigeria");
        validRequest.setBio("Expert in Science");
    }

    @Test
    public void registerTutor_whenValid_thenSavesAndSendsEmail() {
        when(tutorRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(pendingTutorRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encoded-password");

        PendingTutor mockSavedTutor = new PendingTutor();
        mockSavedTutor.setEmail(validRequest.getEmail());
        when(pendingTutorRepository.save(any(PendingTutor.class))).thenReturn(mockSavedTutor);

        when(otpService.generateTutorRegistrationOTP(validRequest.getEmail())).thenReturn("123456");

        PendingTutor result = pendingTutorService.registerTutor(validRequest);

        assertNotNull(result);
        assertEquals(validRequest.getEmail(), result.getEmail());
        verify(pendingTutorRepository, times(1)).save(any(PendingTutor.class));
        verify(kafkaTemplate, times(1)).send(eq("email-topic"), any(EmailEvent.class));
    }

    @Test
    public void registerTutor_whenEmailExistsInMainRepo_thenThrowsBadRequest() {
        // According to your service logic, it checks Main repo first
        when(tutorRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> pendingTutorService.registerTutor(validRequest));
        verify(pendingTutorRepository, never()).save(any());
    }

    @Test
    public void registerTutor_whenEmailExistsInPendingRepo_thenThrowsBadRequest() {
        when(tutorRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(pendingTutorRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> pendingTutorService.registerTutor(validRequest));
    }

    @Test
    public void registerTutor_whenInvalidGender_thenThrowsBadInputException() {
        when(tutorRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(pendingTutorRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);

        validRequest.setGender("Unknown"); // Fails Regex

        BadInputException exception = assertThrows(BadInputException.class, () ->
                pendingTutorService.registerTutor(validRequest));

        assertEquals("Invalid Gender", exception.getMessage());
    }
}
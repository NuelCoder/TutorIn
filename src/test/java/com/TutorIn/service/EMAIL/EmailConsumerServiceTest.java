package com.TutorIn.service.EMAIL;

import com.TutorIn.dto.EmailEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EmailConsumerServiceTest {

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private EmailConsumerService emailConsumerService;

    @Test
    public void consumeEmailEvent_shouldCallEmailSenderServiceWithCorrectData() {
        // Arrange
        EmailEvent event = new EmailEvent("user@tutorin.com", "Welcome", "Hello!", "REGISTRATION");

        // Act
        emailConsumerService.consumeEmailEvent(event);

        // Assert
        verify(emailSenderService, times(1)).sendEmail("user@tutorin.com", "Welcome", "Hello!");
    }
}
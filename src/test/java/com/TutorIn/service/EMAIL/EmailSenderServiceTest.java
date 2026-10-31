package com.TutorIn.service.EMAIL;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailSenderService emailSenderService;

    private MimeMessage mimeMessage;

    @BeforeEach
    public void setUp() {
        // Create a real MimeMessage backed by a blank session so MimeMessageHelper doesn't crash
        Session session = Session.getInstance(new Properties());
        mimeMessage = new MimeMessage(session);
    }

    @Test
    public void sendEmail_whenValid_shouldProcessTemplateAndSend() {
        String to = "user@tutorin.com";
        String subject = "Test Subject";
        String body = "Test Body";
        String mockHtmlContent = "<html><body>Test Body</body></html>";

        when(templateEngine.process(eq("email-template"), any(Context.class))).thenReturn(mockHtmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailSenderService.sendEmail(to, subject, body);

        // Assert
        verify(templateEngine, times(1)).process(eq("email-template"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    public void sendEmail_whenExceptionOccurs_shouldCatchAndLogError() {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("Thymeleaf parsing failed"));

        // Act
        // We do not expect an exception to be thrown out of the method, because of your try-catch block!
        emailSenderService.sendEmail("user@tutorin.com", "Subject", "Body");

        // Assert
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
package com.TutorIn.controller;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.service.LOGIN.UserAccountRecoveryService;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import com.TutorIn.service.USER.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AccountRecoveryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAccountRecoveryService userAccountRecoveryService;

    @MockitoBean
    private UserService userService;

    // Kept mocked to prevent the ApplicationContext from crashing
    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    public void whenInitiateAccountRecovery_thenReturn200() throws Exception {
        String jsonRequest = """
                {
                    "email": "user@tutorin.com"
                }
                """;

        // Mock the void method
        doNothing().when(userAccountRecoveryService).initiateRecovery(eq("user@tutorin.com"));

        mockMvc.perform(post("/api/v1/auth/recovery/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists, a recovery OTP has been sent to your email."));
    }

    @Test
    public void whenConfirmAccountRecovery_thenReturn200AndSetCookie() throws Exception {
        String jsonRequest = """
                {
                    "email": "user@tutorin.com",
                    "otp": "123456"
                }
                """;

        AuthTokens mockTokens = new AuthTokens("mock-access-token", "mock-refresh-token");

        when(userAccountRecoveryService.confirmRecovery(eq("user@tutorin.com"), eq("123456")))
                .thenReturn(mockTokens);

        mockMvc.perform(post("/api/v1/auth/recovery/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(cookie().exists("user_refresh_token"))
                .andExpect(cookie().httpOnly("user_refresh_token", true));
    }
}
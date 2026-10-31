package com.TutorIn.controller;

import com.TutorIn.dto.AuthTokens;
import com.TutorIn.dto.LoginRequest;
import com.TutorIn.dto.PasswordVerificationRequest;
import com.TutorIn.service.LOGIN.UserLoginService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.USER.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Uses your H2 test database configuration
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // We mock the services so we don't need real database records to test HTTP routing
    @MockitoBean
    private UserLoginService userLoginService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    public void whenValidUserLogin_thenReturn200AndSetCookie() throws Exception {
        LoginRequest request = new LoginRequest("user@tutorin.com", "StrongPass123!");
        AuthTokens mockTokens = new AuthTokens("mock-access-token", "mock-refresh-token");

        // Tell the mocked service exactly what to return
        when(userLoginService.loginUser(any(LoginRequest.class))).thenReturn(mockTokens);

        mockMvc.perform(post("/api/v1/auth/user-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(cookie().exists("user_refresh_token"))
                .andExpect(cookie().httpOnly("user_refresh_token", true));
    }

    @Test
    public void whenInvalidCredentials_thenReturn401Unauthorized() throws Exception {
        LoginRequest request = new LoginRequest("user@tutorin.com", "WrongPassword!");

        // Simulate Spring Security throwing a bad credentials exception
        when(userLoginService.loginUser(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/user-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // Validates the 401 failure case requirement
    }

    @Test
    public void whenResetPasswordIsMissingFields_thenReturn400BadRequest() throws Exception {
        // Missing the new password, which should trigger the @Valid failure on the Controller
        PasswordVerificationRequest request = new PasswordVerificationRequest();
        request.setEmail("user@tutorin.com");
        request.setOtp("123456");
        // newPassword is deliberately left null

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Validates the 400 failure case requirement
    }

    @Test
    public void whenAccessingProtectedLogoutWithoutToken_thenReturn401() throws Exception {
        // Attempting to hit logout without a Bearer token in the header
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
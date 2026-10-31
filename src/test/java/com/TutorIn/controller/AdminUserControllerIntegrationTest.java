package com.TutorIn.controller;

import com.TutorIn.service.ADMIN.AdminService;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    // Kept mocked to prevent the ApplicationContext from crashing
    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    @WithMockUser(username = "admin@tutorin.com", authorities = "ROLE_ADMIN")
    public void whenGenerateUserRegistrationOtp_thenReturn200() throws Exception {
        // The username from @WithMockUser is automatically injected into SecurityContextHolder
        when(adminService.adminGenerateManualOtpForUserRegistration(eq("user@tutorin.com"), eq("admin@tutorin.com")))
                .thenReturn("123456");

        mockMvc.perform(post("/api/v1/admins/users/failover/registration-otp")
                        .param("userEmail", "user@tutorin.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration failover OTP generated successfully."))
                .andExpect(jsonPath("$.otp").value("123456"));
    }

    @Test
    @WithMockUser(username = "admin@tutorin.com", authorities = "ROLE_ADMIN")
    public void whenSuspendUser_thenReturn200() throws Exception {
        // Mock the void suspension method
        doNothing().when(adminService).toggleUserSuspension(eq("baduser@tutorin.com"), eq(true), eq("admin@tutorin.com"));

        mockMvc.perform(put("/api/v1/admins/users/suspend")
                        .param("userEmail", "baduser@tutorin.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User account has been successfully suspended."));
    }

    @Test
    @WithMockUser(username = "admin@tutorin.com", authorities = "ROLE_ADMIN")
    public void whenAdminChangeUserEmail_thenReturn200() throws Exception {
        doNothing().when(adminService).adminOverrideUserEmail(
                eq("old@tutorin.com"), eq("new@tutorin.com"), eq("admin@tutorin.com")
        );

        mockMvc.perform(put("/api/v1/admins/users/change-email")
                        .param("currentEmail", "old@tutorin.com")
                        .param("newEmail", "new@tutorin.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User email successfully updated from the admin panel."));
    }

    @Test
    public void whenAccessingAdminFunctionsWithoutAuth_thenReturn403() throws Exception {
        mockMvc.perform(put("/api/v1/admins/users/suspend")
                        .param("userEmail", "baduser@tutorin.com"))
                .andExpect(status().isForbidden());
    }
}
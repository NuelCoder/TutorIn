package com.TutorIn.controller;

import com.TutorIn.dto.PendingAdminDTO;
import com.TutorIn.dto.UpdateAdmin;
import com.TutorIn.entity.Admin;
import com.TutorIn.entity.PendingAdmin;
import com.TutorIn.enums.Role;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.ADMIN.AdminService;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import com.TutorIn.service.PENDINGADMIN.PendingAdminService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private PendingAdminService pendingAdminService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    // Kept mocked to prevent the ApplicationContext from crashing
    @MockitoBean
    private EmailSenderService emailSenderService;

    @MockitoBean
    private EmailConsumerService emailConsumerService;

    @Test
    public void whenRegisterAdmin_thenReturn200() throws Exception {
        // Using Java 17 text blocks to safely mimic the exact JSON payload
        String jsonRequest = """
                {
                    "adminName": "Super Admin",
                    "adminEmail": "admin@tutorin.com",
                    "adminPassword": "StrongAdminPass123!"
                }
                """;

        PendingAdmin mockPendingAdmin = new PendingAdmin();
        mockPendingAdmin.setPendingAdminId(UUID.randomUUID());
        mockPendingAdmin.setAdminName("Super Admin");
        mockPendingAdmin.setAdminEmail("admin@tutorin.com");
        mockPendingAdmin.setRole(Role.PENDING);

        when(pendingAdminService.registerAdmin(any(PendingAdminDTO.class))).thenReturn(mockPendingAdmin);

        mockMvc.perform(post("/api/v1/admins/register-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile has been filled"))
                .andExpect(jsonPath("$.pending.adminEmail").value("admin@tutorin.com"));
    }

    @Test
    @WithMockUser(username = "admin@tutorin.com", authorities = "ROLE_ADMIN") // Injecting the Admin identity
    public void whenGetAdminProfile_thenReturn200() throws Exception {
        Admin mockAdmin = new Admin();
        mockAdmin.setAdminId(UUID.randomUUID());
        mockAdmin.setAdminName("Super Admin");
        mockAdmin.setAdminEmail("admin@tutorin.com");
        mockAdmin.setRole(Role.ADMIN);

        when(adminService.getAdminProfile(anyString())).thenReturn(mockAdmin);

        mockMvc.perform(get("/api/v1/admins/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminName").value("Super Admin"))
                .andExpect(jsonPath("$.adminEmail").value("admin@tutorin.com"));
    }

    @Test
    @WithMockUser(username = "admin@tutorin.com", authorities = "ROLE_ADMIN")
    public void whenUpdateAdminWithInvalidEmail_thenReturn400() throws Exception {
        // Passing an invalid email format to intentionally trigger the @Valid constraints
        String jsonRequest = """
                {
                    "adminName": "Updated Admin",
                    "adminEmail": "invalid-email-format"
                }
                """;

        mockMvc.perform(put("/api/v1/admins/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest()); // Validates the 400 Bad Request failure case
    }

    @Test
    public void whenAccessingAdminProfileWithoutAuth_thenReturn403() throws Exception {
        // No @WithMockUser provided; Spring Security should intercept and forbid access
        mockMvc.perform(get("/api/v1/admins/me"))
                .andExpect(status().isForbidden()); // Validates the 403 Forbidden failure case
    }
}
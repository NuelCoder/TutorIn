package com.TutorIn.controller;

import com.TutorIn.entity.PendingUser;
import com.TutorIn.entity.User;
import com.TutorIn.enums.Role;
import com.TutorIn.security.JwtUtil;
import com.TutorIn.service.EMAIL.EmailConsumerService;
import com.TutorIn.service.EMAIL.EmailSenderService;
import com.TutorIn.service.PENDINGUSER.PendingUserService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import com.TutorIn.service.USER.UserService;
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
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PendingUserService pendingUserService;

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
    public void whenRegisterUser_thenReturn200() throws Exception {
        String jsonRequest = """
                {
                    "name": "Alice Student",
                    "email": "alice@tutorin.com",
                    "gender": "Female",
                    "password": "StrongPassword123!",
                    "country": "Nigeria"
                }
                """;

        PendingUser mockPendingUser = new PendingUser();
        mockPendingUser.setPendingUserId(UUID.randomUUID());
        mockPendingUser.setName("Alice Student");
        mockPendingUser.setEmail("alice@tutorin.com");
        mockPendingUser.setRole(Role.PENDING);
        mockPendingUser.setCreatedAt(OffsetDateTime.now());

        when(pendingUserService.registerUser(any())).thenReturn(mockPendingUser);

        mockMvc.perform(post("/api/v1/users/register-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile has been filled"))
                .andExpect(jsonPath("$.pending.email").value("alice@tutorin.com"));
    }

    @Test
    @WithMockUser(username = "alice@tutorin.com", authorities = "ROLE_USER")
    public void whenGetUserProfile_thenReturn200() throws Exception {
        User mockUser = new User();
        mockUser.setUserId(UUID.randomUUID());
        mockUser.setName("Alice Student");
        mockUser.setEmail("alice@tutorin.com");
        mockUser.setRole(Role.USER);

        when(userService.getUserProfile(anyString())).thenReturn(mockUser);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Student"))
                .andExpect(jsonPath("$.email").value("alice@tutorin.com"));
    }

    @Test
    @WithMockUser(username = "alice@tutorin.com", authorities = "ROLE_USER")
    public void whenUpdateUserWithInvalidEmail_thenReturn400() throws Exception {
        // Intentionally invalid email to trigger the @Valid constraints
        String jsonRequest = """
                {
                    "name": "Alice Updated",
                    "gender": "Female",
                    "email": "not-a-valid-email",
                    "country": "Nigeria"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest()); // Validates the 400 Bad Request failure case
    }

    @Test
    public void whenAccessingUserProfileWithoutAuth_thenReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden()); // Validates the 403 Forbidden failure case
    }
}
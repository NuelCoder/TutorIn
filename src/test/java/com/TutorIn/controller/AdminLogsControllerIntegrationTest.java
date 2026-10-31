package com.TutorIn.controller;

import com.TutorIn.entity.SystemAuditLogs;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminLogsControllerIntegrationTest {

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
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void whenGetAuditLogs_thenReturn200() throws Exception {
        SystemAuditLogs mockLog = new SystemAuditLogs();
        mockLog.setId(UUID.randomUUID());
        mockLog.setAdminEmail("admin@tutorin.com");
        mockLog.setAction("SUSPEND_USER");
        mockLog.setTarget("student@tutorin.com");
        mockLog.setTimestamp(OffsetDateTime.now());

        when(adminService.getRecentAuditLogs()).thenReturn(List.of(mockLog));

        mockMvc.perform(get("/api/v1/admins/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("SUSPEND_USER"))
                .andExpect(jsonPath("$[0].adminEmail").value("admin@tutorin.com"));
    }

    @Test
    public void whenAccessingLogsWithoutAuth_thenReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/admins/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
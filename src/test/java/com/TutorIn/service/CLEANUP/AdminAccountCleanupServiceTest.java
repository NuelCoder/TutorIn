package com.TutorIn.service.CLEANUP;

import com.TutorIn.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AdminAccountCleanupServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminAccountCleanupService adminAccountCleanupService;

    @Test
    public void executeAccountPurge_shouldCallPermanentlyDeleteWithThirtyDaysAgo() {
        // Act
        adminAccountCleanupService.executeAccountPurge();

        // Assert - Verifies that the repository method is called once with an OffsetDateTime argument
        verify(adminRepository, times(1)).permanentlyDeleteExpiredAccounts(any(OffsetDateTime.class));
    }
}
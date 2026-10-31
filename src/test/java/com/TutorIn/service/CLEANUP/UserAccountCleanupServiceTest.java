package com.TutorIn.service.CLEANUP;

import com.TutorIn.repository.UserRepository;
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
public class UserAccountCleanupServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAccountCleanupService userAccountCleanupService;

    @Test
    public void executeAccountPurge_shouldCallPermanentlyDeleteWithThirtyDaysAgo() {
        // Act
        userAccountCleanupService.executeAccountPurge();

        // Assert
        verify(userRepository, times(1)).permanentlyDeleteExpiredAccounts(any(OffsetDateTime.class));
    }
}
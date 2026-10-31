package com.TutorIn.service.CLEANUP;

import com.TutorIn.repository.TutorRepository;
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
public class TutorAccountCleanupServiceTest {

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private TutorAccountCleanupService tutorAccountCleanupService;

    @Test
    public void executeAccountPurge_shouldCallPermanentlyDeleteWithThirtyDaysAgo() {
        // Act
        tutorAccountCleanupService.executeAccountPurge();

        // Assert
        verify(tutorRepository, times(1)).permanentlyDeleteExpiredAccounts(any(OffsetDateTime.class));
    }
}
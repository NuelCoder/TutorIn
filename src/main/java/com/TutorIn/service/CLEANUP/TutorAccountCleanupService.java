package com.TutorIn.service.CLEANUP;

import com.TutorIn.repository.TutorRepository;
import com.TutorIn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TutorAccountCleanupService {

    private final TutorRepository tutorRepository;
    private final Logger log = LoggerFactory.getLogger(TutorAccountCleanupService.class);

    // "Run at exactly 00:00 (Midnight) every single day"
    @Scheduled(cron = "0 0 0 * * ?")
    public void executeAccountPurge() {
        log.info("Starting nightly purge of expired deleted accounts...");

        // Calculate the exact date 30 days ago from right now
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);

        // Fire the Hard Delete query
        tutorRepository.permanentlyDeleteExpiredAccounts(thirtyDaysAgo);

        log.info("Nightly account purge complete.");
    }
}

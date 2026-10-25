package com.TutorIn.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportResponse(
        UUID reportId,
        String reporterEmail,
        String reportedEmail,
        UUID sessionId,
        String reason,
        String adminResolutionNotes,
        String status,
        OffsetDateTime createdAt
) {
}

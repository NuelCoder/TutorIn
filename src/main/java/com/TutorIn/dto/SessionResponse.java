package com.TutorIn.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionResponse (
        UUID sessionId,
        UUID slotId,
        String tutorName,
        String studentName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status
){
}

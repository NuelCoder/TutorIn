package com.TutorIn.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        UUID tutorId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        boolean isBooked) {
}

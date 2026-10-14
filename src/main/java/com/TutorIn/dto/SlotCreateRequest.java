package com.TutorIn.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SlotCreateRequest(
        @NotNull(message = "Tutor ID is required")
        UUID tutorId,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        OffsetDateTime startTime,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        OffsetDateTime endTime
) {
}

package com.TutorIn.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record SlotUpdateRequest(
        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        OffsetDateTime startTime,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        OffsetDateTime endTime
) {
}

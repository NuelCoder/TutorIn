package com.TutorIn.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BookSessionRequest(
        @NotNull(message = "Slot ID is required") UUID slotId
) {
}

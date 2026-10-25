package com.TutorIn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TutorSubjectRequest(
        @NotNull(message = "Subject ID is required") UUID subjectId,
        @NotNull(message = "Hourly rate is required") @Positive(message = "Rate must be positive") BigDecimal hourlyRate
) {
}

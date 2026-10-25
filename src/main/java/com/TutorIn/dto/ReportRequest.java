package com.TutorIn.dto;

import com.TutorIn.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReportRequest(
        @Email(message = "Invalid email format") @NotBlank String reportedEmail,
        @NotNull(message = "Reported role type is required") RoleType reportedType,
        UUID sessionId,
        @NotBlank(message = "Reason is required") String reason
) {
}

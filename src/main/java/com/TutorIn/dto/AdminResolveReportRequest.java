package com.TutorIn.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminResolveReportRequest(
        @NotBlank(message = "Resolution notes are required") String adminResolutionNotes,
        @NotBlank(message = "Status is required (e.g., RESOLVED, DISMISSED)") String status
) {
}

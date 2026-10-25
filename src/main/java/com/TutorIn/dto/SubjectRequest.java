package com.TutorIn.dto;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank(message = "Subject title is required") String subjectTitle,
        @NotBlank(message = "Description is required") String description) {
}

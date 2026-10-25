package com.TutorIn.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TutorSearchResponse (
        UUID tutorId,
        String tutorName,
        String bio,
        String country,
        String subjectTitle,
        BigDecimal hourlyRate
){
}

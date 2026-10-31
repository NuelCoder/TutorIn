package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlotUpdateRequestTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        SlotUpdateRequest request = new SlotUpdateRequest(
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2)
        );
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    public void whenEndTimeIsInPast_thenValidationFails() {
        SlotUpdateRequest request = new SlotUpdateRequest(
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().minusDays(1) // End time in the past
        );
        Set<ConstraintViolation<SlotUpdateRequest>> violations = validator.validate(request);
        assertEquals("End time must be in the future", violations.iterator().next().getMessage());
    }
}
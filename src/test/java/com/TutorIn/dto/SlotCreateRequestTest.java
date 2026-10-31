package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlotCreateRequestTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        SlotCreateRequest request = new SlotCreateRequest(UUID.randomUUID(), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1));
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    public void whenStartTimeIsInPast_thenValidationFails() {
        SlotCreateRequest request = new SlotCreateRequest(UUID.randomUUID(), OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1));
        Set<ConstraintViolation<SlotCreateRequest>> violations = validator.validate(request);
        assertEquals("Start time must be in the future", violations.iterator().next().getMessage());
    }
}
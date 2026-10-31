package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TutorSubjectRequestTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        TutorSubjectRequest request = new TutorSubjectRequest(UUID.randomUUID(), new BigDecimal("25.00"));
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    public void whenHourlyRateIsNegative_thenValidationFails() {
        TutorSubjectRequest request = new TutorSubjectRequest(UUID.randomUUID(), new BigDecimal("-10.00"));
        Set<ConstraintViolation<TutorSubjectRequest>> violations = validator.validate(request);
        assertEquals("Rate must be positive", violations.iterator().next().getMessage());
    }
}
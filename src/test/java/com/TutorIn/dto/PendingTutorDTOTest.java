package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PendingTutorDTOTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        PendingTutorDTO dto = new PendingTutorDTO(
                "Jane Doe", "Math Expert", "jane@tutorin.com", "Female", "StrongPass123!", "Nigeria"
        );
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    public void whenEmailIsInvalid_thenValidationFails() {
        PendingTutorDTO dto = new PendingTutorDTO(
                "Jane Doe", "Math Expert", "jane-at-tutorin.com", "Female", "StrongPass123!", "Nigeria"
        );
        Set<ConstraintViolation<PendingTutorDTO>> violations = validator.validate(dto);
        assertEquals("Please provide a valid email", violations.iterator().next().getMessage());
    }
}
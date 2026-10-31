package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PendingUserDTOTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        PendingUserDTO dto = new PendingUserDTO(
                "John Doe", "john@tutorin.com", "StrongPass123!", "Male", "Nigeria"
        );
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    public void whenPasswordIsWeak_thenValidationFails() {
        PendingUserDTO dto = new PendingUserDTO(
                "John Doe", "john@tutorin.com", "weakpass", "Male", "Nigeria"
        );
        Set<ConstraintViolation<PendingUserDTO>> violations = validator.validate(dto);
        assertEquals("Password must be at least 8 characters long and contain uppercase, lowercase, digit, and special character", violations.iterator().next().getMessage());
    }
}
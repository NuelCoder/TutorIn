package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PendingAdminDTOTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        PendingAdminDTO dto = new PendingAdminDTO("Admin Name", "admin@test.com", "StrongPass123!");
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    public void whenPasswordIsWeak_thenFailsRegex() {
        PendingAdminDTO dto = new PendingAdminDTO("Admin Name", "admin@test.com", "weak");
        Set<ConstraintViolation<PendingAdminDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }
}
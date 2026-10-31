package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateAdminTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        UpdateAdmin admin = new UpdateAdmin("Jane Doe", "jane@tutorin.com");
        assertTrue(validator.validate(admin).isEmpty());
    }

    @Test
    public void whenEmailIsInvalid_thenValidationFails() {
        UpdateAdmin admin = new UpdateAdmin("Jane Doe", "jane-no-domain");
        Set<ConstraintViolation<UpdateAdmin>> violations = validator.validate(admin);
        assertEquals("Please provide a valid email", violations.iterator().next().getMessage());
    }
}
package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateUserTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        UpdateUser user = new UpdateUser("Alice", "Female", "alice@tutorin.com", null);
        assertTrue(validator.validate(user).isEmpty());
    }

    @Test
    public void whenEmailIsInvalid_thenValidationFails() {
        UpdateUser user = new UpdateUser("Alice", "Female", "alice-at-tutorin.com", null);
        Set<ConstraintViolation<UpdateUser>> violations = validator.validate(user);
        assertEquals("Please provide a valid email", violations.iterator().next().getMessage());
    }
}
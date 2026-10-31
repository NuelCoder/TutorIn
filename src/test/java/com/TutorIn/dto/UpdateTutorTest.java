package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateTutorTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        UpdateTutor tutor = new UpdateTutor("John", "Male", "Bio here", "john@tutorin.com", null);
        assertTrue(validator.validate(tutor).isEmpty());
    }

    @Test
    public void whenEmailIsInvalid_thenValidationFails() {
        UpdateTutor tutor = new UpdateTutor("John", "Male", "Bio here", "invalid-email", null);
        Set<ConstraintViolation<UpdateTutor>> violations = validator.validate(tutor);
        assertEquals("Please provide a valid email", violations.iterator().next().getMessage());
    }
}
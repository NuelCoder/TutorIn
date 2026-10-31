package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubjectRequestTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        SubjectRequest request = new SubjectRequest("Mathematics", "Advanced Calculus");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    public void whenTitleIsBlank_thenValidationFails() {
        SubjectRequest request = new SubjectRequest("", "Advanced Calculus");
        Set<ConstraintViolation<SubjectRequest>> violations = validator.validate(request);
        assertEquals("Subject title is required", violations.iterator().next().getMessage());
    }
}
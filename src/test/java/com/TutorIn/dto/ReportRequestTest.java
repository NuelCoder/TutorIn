package com.TutorIn.dto;

import com.TutorIn.enums.RoleType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReportRequestTest {
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void whenValid_thenNoViolations() {
        ReportRequest request = new ReportRequest("baduser@test.com", RoleType.USER, UUID.randomUUID(), "Inappropriate behavior");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    public void whenEmailIsInvalid_thenValidationFails() {
        ReportRequest request = new ReportRequest("not-an-email", RoleType.USER, UUID.randomUUID(), "Reason");
        Set<ConstraintViolation<ReportRequest>> violations = validator.validate(request);
        assertEquals("Invalid email format", violations.iterator().next().getMessage());
    }
}
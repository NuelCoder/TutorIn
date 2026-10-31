package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminResolveReportRequestTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void whenAdminResolveReportRequestIsValid_thenNoViolations() {
        AdminResolveReportRequest request = new AdminResolveReportRequest(
                "User was warned regarding the violation.",
                "RESOLVED"
        );

        Set<ConstraintViolation<AdminResolveReportRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should not have violations");
    }

    @Test
    public void whenAdminResolutionNotesIsBlank_thenValidationFails() {
        AdminResolveReportRequest request = new AdminResolveReportRequest(
                "", // Blank notes
                "RESOLVED"
        );

        Set<ConstraintViolation<AdminResolveReportRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Resolution notes are required", violations.iterator().next().getMessage());
    }

    @Test
    public void whenAdminResolutionStatusIsBlank_thenValidationFails() {
        AdminResolveReportRequest request = new AdminResolveReportRequest(
                "User was warned.",
                "" // Blank status
        );

        Set<ConstraintViolation<AdminResolveReportRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Status is required (e.g., RESOLVED, DISMISSED)", violations.iterator().next().getMessage());
    }
}
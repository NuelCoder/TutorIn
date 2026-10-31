package com.TutorIn.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookSessionRequestTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void whenBookSessionRequestIsValid_thenNoViolations() {
        BookSessionRequest request = new BookSessionRequest(UUID.randomUUID());

        Set<ConstraintViolation<BookSessionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should not have violations");
    }

    @Test
    public void whenBookSessionRequestSlotIdIsNull_thenValidationFails() {
        BookSessionRequest request = new BookSessionRequest(null);

        Set<ConstraintViolation<BookSessionRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Slot ID is required", violations.iterator().next().getMessage());
    }
}
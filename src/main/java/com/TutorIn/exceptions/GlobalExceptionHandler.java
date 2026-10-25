package com.TutorIn.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> formatError(String code, String message, HttpStatus status) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("code", code);
        errorDetails.put("message", message);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorDetails);

        return new ResponseEntity<>(errorResponse, status);
    }


    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return formatError("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadInputException.class)
    public ResponseEntity<Map<String, Object>> handleBadInput(BadInputException ex) {
        return formatError("BAD_INPUT", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return formatError("FORBIDDEN", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(TooManyRequestException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestException ex) {
        return formatError("TOO_MANY_REQUESTS", ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return formatError("UNAUTHORIZED", "Incorrect email or password.", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccountPendingRecoveryException.class)
    public ResponseEntity<Map<String, Object>> handlePendingRecovery(AccountPendingRecoveryException ex) {
        return formatError("PENDING_RECOVERY", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccountPermanentlyDeletedException.class)
    public ResponseEntity<Map<String, Object>> handleDeletedAccount(AccountPermanentlyDeletedException ex) {
        return formatError("ACCOUNT_DELETED", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorMessage.append(fieldError.getField()).append(" ").append(fieldError.getDefaultMessage()).append("; ");
        }
        return formatError("VALIDATION_ERROR", errorMessage.toString(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        System.err.println("🚨 UNEXPECTED EXCEPTION: " + ex.getMessage());
        ex.printStackTrace();

        return formatError(
                "INTERNAL_SERVER_ERROR",
                "An unexpected server error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}


package com.TutorIn.exceptions;

public class AccountPendingRecoveryException extends RuntimeException {
    public AccountPendingRecoveryException(String message) {
        super(message);
    }
}

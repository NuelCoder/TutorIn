package com.TutorIn.exceptions;

public class AccountPermanentlyDeletedException extends RuntimeException {
    public AccountPermanentlyDeletedException(String message) {
        super(message);
    }
}

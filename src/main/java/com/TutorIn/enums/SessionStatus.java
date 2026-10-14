package com.TutorIn.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SessionStatus {
    BOOKED("BOOKED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String displayName;

    SessionStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static SessionStatus fromValue(String value) {
        for (SessionStatus sessionStatus : SessionStatus.values()) {
            if (sessionStatus.displayName.equalsIgnoreCase(value)) {
                return sessionStatus;
            }
        }
        throw new IllegalArgumentException("Invalid role: " + value);
    }
}
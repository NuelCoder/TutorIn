package com.TutorIn.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoleType {
    USER("User"),
    TUTOR("Tutor");

    private final String displayName;

    RoleType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static RoleType fromValue(String value) {
        for (RoleType roleType : RoleType.values()) {
            if (roleType.displayName.equalsIgnoreCase(value)) {
                return roleType;
            }
        }
        throw new IllegalArgumentException("Invalid role: " + value);
    }
}

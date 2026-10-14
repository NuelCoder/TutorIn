package com.TutorIn.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportStatus {
    PENDING("PENDING"),
    REVIEWING("REVIEWING"),
    RESOLVED("RESOLVED"),
    DISMISSED("DISMISSED");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static ReportStatus fromValue(String value) {
        for (ReportStatus reportStatus : ReportStatus.values()) {
            if (reportStatus.displayName.equalsIgnoreCase(value)) {
                return reportStatus;
            }
        }
        throw new IllegalArgumentException("Invalid role: " + value);
    }
}


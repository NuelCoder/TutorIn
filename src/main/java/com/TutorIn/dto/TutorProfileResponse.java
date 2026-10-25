package com.TutorIn.dto;

import com.TutorIn.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class TutorProfileResponse {
    private UUID tutorId;
    private String name;
    private String bio;
    private String email;
    private String gender;
    private String country;
    private Role role;
    private OffsetDateTime createdAt;
}

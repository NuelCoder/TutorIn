package com.TutorIn.dto;

import com.TutorIn.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminProfileResponse {
    private String adminName;
    private String adminEmail;
    private Role role;
    private LocalDateTime createdAt;
}

package com.TutorIn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordVerificationRequest {

    @NotBlank(message = "Email is required")
    private String email;

    private String oldPassword; // Left without @NotBlank since forgot-password doesn't use it

    @NotBlank(message = "New password is required")
    private String newPassword;

    @NotBlank(message = "OTP is required")
    private String otp;
}
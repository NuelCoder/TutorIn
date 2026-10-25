package com.TutorIn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordVerificationRequest {
    private String email;
    private String oldPassword;
    private String newPassword;
    private String otp;
}

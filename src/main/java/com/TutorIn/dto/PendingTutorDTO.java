package com.TutorIn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PendingTutorDTO {

    @NotBlank(message = "Please provide a name")
    private String name;

    @NotBlank(message = "Please provide a bio")
    private String bio;

    @NotBlank(message = "Please provide an email")
    @Email(message = "Please provide a valid email")
    private String email;

    @NotBlank(message = "Please provide your gender")
    private String gender;

    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.\\-_#])[A-Za-z0-9@$!%*&?.\\-_#]{8,}$",
            message = "Password must be at least 8 characters long and contain uppercase, lowercase, digit, and special character"
    )
    @NotBlank(message = "Please provide a password")
    private String password;

    @NotBlank(message = "Please provide a country")
    private String country;
}

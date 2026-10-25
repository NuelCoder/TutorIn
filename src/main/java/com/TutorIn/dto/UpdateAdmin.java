package com.TutorIn.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAdmin {

    private String adminName;

    @Email(message = "Please provide a valid email")
    private String adminEmail;
}

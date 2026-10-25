package com.TutorIn.dto;

import com.TutorIn.validation.ValidCountry;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUser {
    private String name;

    private String gender;

    @Email(message = "Please provide a valid email")
    private String email;

    @ValidCountry(message = "Please provide a valid, recognized country name.")
    private String country;
}

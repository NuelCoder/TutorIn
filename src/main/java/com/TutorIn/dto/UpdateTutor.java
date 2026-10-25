package com.TutorIn.dto;

import com.TutorIn.validation.ValidCountry;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTutor {
    private String name;

    private String gender;

    private String bio;

    @Email(message = "Please provide a valid email")
    private String email;

    @ValidCountry(message = "Please provide a valid, recognized country name.")
    private String country;
}

package com.TutorIn.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "pending_tutors")
@AllArgsConstructor
@NoArgsConstructor
public class PendingTutor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pendingTutorId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bio;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Please provide an email")
    @Email(message = "Please provide a valid email!")
    private String email;

    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]).{8,}$",
            message = "Password must be at least 8 characters long and contain uppercase, lowercase, digit, and special character"
    )
    @Column(nullable = false)
    @NotBlank(message = "Please provide a password")
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    @NotBlank(message = "Please provide a country")
    private String country;

    private String otp;
}

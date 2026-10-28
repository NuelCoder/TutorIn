package com.TutorIn.entity;

import com.TutorIn.enums.Role;
import com.TutorIn.validation.ValidCountry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
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

    @Column(nullable = false)
    @NotBlank(message = "Please provide your gender")
    private String gender;

    @Column(nullable = false)
    @NotBlank(message = "Please provide a password")
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    @NotBlank(message = "Please provide a country")
    @ValidCountry(message = "Please provide a valid, recognized country name.")
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    private String otp;
}

package com.TutorIn.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "pending_users")
@AllArgsConstructor
@NoArgsConstructor
public class PendingUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pendingUserId;

    @Column(nullable = false)
    @NotBlank(message = "Please provide your name")
    private String name;

    @Column(nullable = false)
    @NotBlank(message = "Please provide your gender")
    private String gender;

    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.\\-_#])[A-Za-z0-9@$!%*&?.\\-_#]{8,}$",
            message = "Password must be at least 8 characters long and contain uppercase, lowercase, digit, and special character"
    )
    @Column(nullable = false)
    @NotBlank(message = "Please provide a password")
    private String password;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Please provide an email")
    @Email(message = "Please provide a valid email")
    private String email;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String otp;

    @Column(nullable = false)
    private String country;
}

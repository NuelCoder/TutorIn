package com.TutorIn.entity;

import com.TutorIn.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@Getter
@Setter
@Table(name = "pending_admin")
@AllArgsConstructor
@NoArgsConstructor
public class PendingAdmin {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pendingAdminId;

    @Column(nullable = false)
    @NotBlank(message = "Please provide your name")
    private String adminName;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Please provide your email")
    @Email(message = "Please provide a valid email")
    private String adminEmail;

    @Column(nullable = false)
    @NotBlank(message = "Please provide your password")
    private String adminPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String otp;
}

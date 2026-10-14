package com.TutorIn.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "subjects")
@AllArgsConstructor
@NoArgsConstructor
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID subjectId;

    @Column(nullable = false,unique = true)
    @NotBlank(message = "Please provide a subject title")
    private String subjectTitle;

    private String description;
}

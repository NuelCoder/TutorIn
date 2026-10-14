package com.TutorIn.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TutorSubjectId implements Serializable {
    private UUID tutorId;
    private UUID subjectId;
}

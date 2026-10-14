package com.TutorIn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tutor_subjects")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TutorSubject {

    @EmbeddedId
    private TutorSubjectId id = new TutorSubjectId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tutorId")
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("subjectId")
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}

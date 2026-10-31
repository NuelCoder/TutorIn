package com.TutorIn.entity;

import com.TutorIn.enums.ReportStatus;
import com.TutorIn.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_reported_user", columnList = "reported_user_id"),
        @Index(name = "idx_report_reported_tutor", columnList = "reported_tutor_id")
})
@AllArgsConstructor
@NoArgsConstructor
public class Reports {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reportId;

    // --- Reporter ---
    @Enumerated(EnumType.STRING)
    @Column(name = "reporter_type", nullable = false)
    private RoleType reporterType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id")
    private User reporterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_tutor_id")
    private Tutor reporterTutor;

    // --- Reported (The accused) ---
    @Enumerated(EnumType.STRING)
    @Column(name = "reported_type", nullable = false)
    private RoleType reportedType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_tutor_id")
    private Tutor reportedTutor;

    // --- Context ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String adminResolutionNotes; // For the Admin to log action taken

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
}

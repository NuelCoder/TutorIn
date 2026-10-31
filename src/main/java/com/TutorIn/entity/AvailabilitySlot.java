package com.TutorIn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "availability_slots", indexes = {
        @Index(name = "idx_slot_tutor", columnList = "tutor_id"),
        @Index(name = "idx_slot_start_time", columnList = "start_time")
})
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilitySlot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "is_booked", nullable = false)
    private boolean isBooked = false;
}

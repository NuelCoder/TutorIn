package com.TutorIn.repository;

import com.TutorIn.entity.AvailabilitySlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {
    List<AvailabilitySlot> findByTutor_TutorIdOrderByStartTimeAsc(UUID tutorId);

    // Overlap check for CREATING a new slot
    @Query("""
        SELECT COUNT(s) FROM AvailabilitySlot s 
        WHERE s.tutor.tutorId = :tutorId 
          AND s.startTime < :endTime 
          AND s.endTime > :startTime
    """)
    long countOverlappingSlots(
            @Param("tutorId") UUID tutorId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    // Overlap check for UPDATING an existing slot (ignores the slot being updated)
    @Query("""
        SELECT COUNT(s) FROM AvailabilitySlot s 
        WHERE s.tutor.tutorId = :tutorId 
          AND s.id != :slotId 
          AND s.startTime < :endTime 
          AND s.endTime > :startTime
    """)
    long countOverlappingSlotsExcludingSelf(
            @Param("tutorId") UUID tutorId,
            @Param("slotId") UUID slotId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.id = :slotId")
    Optional<AvailabilitySlot> findByIdWithPessimisticLock(@Param("slotId") UUID slotId);
}

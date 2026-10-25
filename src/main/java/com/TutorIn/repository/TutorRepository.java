package com.TutorIn.repository;

import com.TutorIn.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, UUID> {
    Optional<Tutor> findByEmail(String email);
    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM tutors WHERE deleted = true AND updated_at < ?1", nativeQuery = true)
    void permanentlyDeleteExpiredAccounts(OffsetDateTime cutoffDate);

    @Query(value = "SELECT * FROM tutors WHERE email = ?1", nativeQuery = true)
    Optional<Tutor> findAnyByEmailIncludingDeleted(String email);
}

package com.TutorIn.repository;

import com.TutorIn.entity.PendingAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PendingAdminRepository extends JpaRepository<PendingAdmin, UUID> {
    Optional<PendingAdmin> findByAdminEmail(String adminEmail);
    boolean existsByAdminEmail(String adminEmail);
    Optional<PendingAdmin>findByPendingAdminId(UUID pendingAdminId);
}

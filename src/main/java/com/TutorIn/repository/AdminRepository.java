package com.TutorIn.repository;

import com.TutorIn.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {
    Optional<Admin> findByAdminEmail(String adminEmail);
    boolean existsByAdminEmail(String adminEmail);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM admins WHERE deleted = true AND updated_at < ?1", nativeQuery = true)
    void permanentlyDeleteExpiredAccounts(OffsetDateTime cutoffDate);

    @Query(value = "SELECT * FROM admins WHERE adminEmail = ?1", nativeQuery = true)
    Optional<Admin> findAnyByAdminEmailIncludingDeleted(String email);
}

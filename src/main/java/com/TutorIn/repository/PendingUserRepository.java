package com.TutorIn.repository;

import com.TutorIn.entity.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, UUID> {
    Optional<PendingUser> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<PendingUser>findByPendingUserId(UUID pendingUserId);
}

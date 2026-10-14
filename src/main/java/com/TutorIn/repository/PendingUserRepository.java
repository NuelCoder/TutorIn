package com.TutorIn.repository;

import com.TutorIn.entity.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, UUID> {
}

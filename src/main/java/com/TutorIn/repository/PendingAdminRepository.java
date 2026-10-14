package com.TutorIn.repository;

import com.TutorIn.entity.PendingAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PendingAdminRepository extends JpaRepository<PendingAdmin, UUID> {
}

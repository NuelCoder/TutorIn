package com.TutorIn.repository;

import com.TutorIn.entity.PendingTutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PendingTutorRepository extends JpaRepository<PendingTutor, UUID> {
}

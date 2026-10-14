package com.TutorIn.repository;

import com.TutorIn.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, UUID> {
    Optional<Tutor> findByEmail(String email);
    boolean existsByEmail(String email);
}

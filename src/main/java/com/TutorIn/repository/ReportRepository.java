package com.TutorIn.repository;

import com.TutorIn.entity.Reports;
import com.TutorIn.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Reports, UUID> {

    List<Reports> findByStatusOrderByCreatedAtAsc(ReportStatus status);

    // Check if a specific session has already been reported to prevent spam
    boolean existsBySessionSessionId(UUID sessionId);
}

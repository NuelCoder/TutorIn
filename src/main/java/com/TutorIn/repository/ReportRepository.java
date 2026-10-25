package com.TutorIn.repository;

import com.TutorIn.entity.Reports;
import com.TutorIn.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Reports, UUID> {

    // 1. Find all reports currently pending or in-review
    List<Reports> findByStatus(ReportStatus status);

    // 2. Find every report made AGAINST a specific email (checks both User and Tutor foreign keys)
    @Query("""
        SELECT r FROM Reports r 
        WHERE r.reportedUser.email = :email 
           OR r.reportedTutor.email = :email
        ORDER BY r.createdAt DESC
    """)
    List<Reports> findReportsAgainstEmail(@Param("email") String email);

    // 3. Find every report filed BY a specific email
    @Query("""
        SELECT r FROM Reports r 
        WHERE r.reporterUser.email = :email 
           OR r.reporterTutor.email = :email
        ORDER BY r.createdAt DESC
    """)
    List<Reports> findReportsFiledByEmail(@Param("email") String email);
}

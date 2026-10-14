package com.TutorIn.repository;

import com.TutorIn.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    @Query("""
        SELECT s FROM Session s 
        JOIN FETCH s.slot sl 
        JOIN FETCH sl.tutor 
        WHERE s.user.id = :userId 
          AND s.status = 'BOOKED' 
          AND sl.startTime > CURRENT_TIMESTAMP 
        ORDER BY sl.startTime ASC
    """)
    List<Session> findUpcomingSessionsForStudent(@Param("studentId") Long studentId);

    @Query("""
        SELECT s FROM Session s 
        JOIN FETCH s.slot sl 
        JOIN FETCH s.user
        WHERE sl.tutor.id = :tutorId 
          AND s.status = 'BOOKED' 
          AND sl.startTime > CURRENT_TIMESTAMP 
        ORDER BY sl.startTime ASC
    """)
    List<Session> findUpcomingSessionsForTutor(@Param("tutorId") Long tutorId);
}

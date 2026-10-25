package com.TutorIn.repository;

import com.TutorIn.entity.Tutor;
import com.TutorIn.entity.TutorSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface TutorSubjectRepository extends JpaRepository<TutorSubject, UUID> {

    List<TutorSubject> findByTutorTutorId(UUID tutorId);

    @Query("""
        SELECT ts FROM TutorSubject ts
        JOIN FETCH ts.tutor t
        WHERE ts.subject.id = :subjectId
          AND t.active = true
    """)
    List<TutorSubject> findActiveTutorsBySubjectId(@Param("subjectId") Long subjectId);

    // 3. Find tutors for a subject within a specific student budget
    @Query("""
        SELECT ts FROM TutorSubject ts
        JOIN FETCH ts.tutor t
        WHERE ts.subject.id = :subjectId
          AND ts.hourlyRate <= :maxRate
          AND t.active = true
        ORDER BY ts.hourlyRate ASC
    """)
    List<TutorSubject> findTutorsBySubjectAndBudget(
            @Param("subjectId") Long subjectId,
            @Param("maxRate") BigDecimal maxRate
    );

    @Query("""
    SELECT ts FROM TutorSubject ts 
    JOIN FETCH ts.tutor t
    WHERE ts.subject.subjectTitle ILIKE :subjectTitle 
      AND t.active = true
""")
    List<TutorSubject> findActiveTutorSubjectsBySubjectTitle(@Param("subjectTitle") String subjectTitle);

    // Add this inside TutorSubjectRepository
    boolean existsBySubject_SubjectId(UUID subjectId);
}

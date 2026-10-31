package com.TutorIn.service.TUTORSUBJECT;

import com.TutorIn.dto.TutorSubjectRequest;
import com.TutorIn.entity.Subject;
import com.TutorIn.entity.Tutor;
import com.TutorIn.entity.TutorSubject;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.SubjectRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.repository.TutorSubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TutorSubjectServiceTest {

    @Mock private TutorSubjectRepository tutorSubjectRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private SubjectRepository subjectRepository;

    @InjectMocks
    private TutorSubjectService tutorSubjectService;

    @Test
    public void assignSubjectToTutor_whenValid_thenSavesTutorSubject() {
        String email = "tutor@tutorin.com";
        UUID subjectId = UUID.randomUUID();
        TutorSubjectRequest request = new TutorSubjectRequest(subjectId, BigDecimal.valueOf(25.50));

        Tutor mockTutor = new Tutor();
        mockTutor.setTutorId(UUID.randomUUID());
        Subject mockSubject = new Subject();
        mockSubject.setSubjectId(subjectId);

        when(tutorRepository.findByEmail(email)).thenReturn(Optional.of(mockTutor));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(mockSubject));

        tutorSubjectService.assignSubjectToTutor(email, request);

        verify(tutorSubjectRepository, times(1)).save(any(TutorSubject.class));
    }

    @Test
    public void assignSubjectToTutor_whenAlreadyAssigned_thenThrowsBadRequest() {
        String email = "tutor@tutorin.com";
        UUID subjectId = UUID.randomUUID();
        TutorSubjectRequest request = new TutorSubjectRequest(subjectId, BigDecimal.valueOf(25.50));

        Tutor mockTutor = new Tutor();
        mockTutor.setTutorId(UUID.randomUUID());
        Subject mockSubject = new Subject();
        mockSubject.setSubjectId(subjectId);

        TutorSubject existingLink = new TutorSubject();
        existingLink.setSubject(mockSubject);
        mockTutor.getTutorSubjects().add(existingLink); // Subject is already in the list

        when(tutorRepository.findByEmail(email)).thenReturn(Optional.of(mockTutor));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(mockSubject));

        assertThrows(BadRequestException.class, () -> tutorSubjectService.assignSubjectToTutor(email, request));
        verify(tutorSubjectRepository, never()).save(any());
    }
}
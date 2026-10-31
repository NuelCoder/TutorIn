package com.TutorIn.service.SUBJECT;

import com.TutorIn.dto.SubjectRequest;
import com.TutorIn.entity.Subject;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.SubjectRepository;
import com.TutorIn.repository.TutorSubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubjectServiceTest {

    @Mock private SubjectRepository subjectRepository;
    @Mock private TutorSubjectRepository tutorSubjectRepository;

    @InjectMocks
    private SubjectService subjectService;

    @Test
    public void createSubject_whenNotExists_thenSavesSubject() {
        SubjectRequest request = new SubjectRequest("Mathematics", "Math course");
        when(subjectRepository.existsBySubjectTitleIgnoreCase("Mathematics")).thenReturn(false);

        Subject mockSaved = new Subject();
        mockSaved.setSubjectTitle("Mathematics");
        when(subjectRepository.save(any(Subject.class))).thenReturn(mockSaved);

        Subject result = subjectService.createSubject(request);

        assertEquals("Mathematics", result.getSubjectTitle());
        verify(subjectRepository, times(1)).save(any(Subject.class));
    }

    @Test
    public void createSubject_whenExists_thenThrowsBadRequest() {
        SubjectRequest request = new SubjectRequest("Mathematics", "Math course");
        when(subjectRepository.existsBySubjectTitleIgnoreCase("Mathematics")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> subjectService.createSubject(request));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    public void deleteSubject_whenInUseByTutors_thenThrowsBadRequest() {
        UUID subjectId = UUID.randomUUID();
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(new Subject()));
        when(tutorSubjectRepository.existsBySubject_SubjectId(subjectId)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                subjectService.deleteSubject(subjectId));

        assertEquals("Cannot delete this subject because it is currently assigned to one or more tutors.", exception.getMessage());
        verify(subjectRepository, never()).delete(any());
    }
}
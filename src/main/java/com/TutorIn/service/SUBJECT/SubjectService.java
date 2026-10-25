package com.TutorIn.service.SUBJECT;

import com.TutorIn.dto.SubjectRequest;
import com.TutorIn.entity.Subject;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.SubjectRepository;
import com.TutorIn.repository.TutorSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final TutorSubjectRepository tutorSubjectRepository;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Subject createSubject(SubjectRequest request) {
        // Prevent duplicate subjects
        if (subjectRepository.existsBySubjectTitleIgnoreCase(request.subjectTitle())) {
            throw new BadRequestException("Subject already exists.");
        }

        Subject subject = new Subject();
        subject.setSubjectTitle(request.subjectTitle());
        subject.setDescription(request.description());
        return subjectRepository.save(subject);
    }

    @PreAuthorize("isAuthenticated()")
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Subject updateSubject(UUID subjectId, SubjectRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BadRequestException("Subject not found."));

        if (!subject.getSubjectTitle().equalsIgnoreCase(request.subjectTitle()) &&
                subjectRepository.existsBySubjectTitleIgnoreCase(request.subjectTitle())) {
            throw new BadRequestException("Another subject with this title already exists.");
        }
        if (Objects.nonNull(request.subjectTitle())){
            subject.setSubjectTitle(request.subjectTitle());
        }
        if (Objects.nonNull(request.description())){
            subject.setDescription(request.description());
        }


        return subjectRepository.save(subject);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSubject(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BadRequestException("Subject not found."));

        // Edge Case Protection: Prevent deleting a subject if Tutors rely on it
        if (tutorSubjectRepository.existsBySubject_SubjectId(subjectId)) {
            throw new BadRequestException("Cannot delete this subject because it is currently assigned to one or more tutors.");
        }

        subjectRepository.delete(subject);
        }
}
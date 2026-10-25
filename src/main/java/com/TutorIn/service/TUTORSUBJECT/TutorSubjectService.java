package com.TutorIn.service.TUTORSUBJECT;

import com.TutorIn.dto.TutorSearchResponse;
import com.TutorIn.dto.TutorSubjectRequest;
import com.TutorIn.entity.Subject;
import com.TutorIn.entity.Tutor;
import com.TutorIn.entity.TutorSubject;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.SubjectRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.repository.TutorSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TutorSubjectService {

    private final TutorSubjectRepository tutorSubjectRepository;
    private final TutorRepository tutorRepository;
    private final SubjectRepository subjectRepository;

    @Transactional
    @PreAuthorize("hasRole('TUTOR')")
    public void assignSubjectToTutor(String tutorEmail, TutorSubjectRequest request) {
        Tutor tutor = tutorRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new BadRequestException("Tutor not found"));

        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new BadRequestException("Subject not found"));

        boolean alreadyExists = tutor.getTutorSubjects().stream()
                .anyMatch(ts -> ts.getSubject().getSubjectId().equals(subject.getSubjectId()));

        if (alreadyExists) {
            throw new BadRequestException("You are already registered to teach this subject.");
        }

        TutorSubject tutorSubject = new TutorSubject();
        tutorSubject.setTutor(tutor);
        tutorSubject.setSubject(subject);
        tutorSubject.setHourlyRate(request.hourlyRate());

        tutorSubject.getId().setTutorId(tutor.getTutorId());
        tutorSubject.getId().setSubjectId(subject.getSubjectId());

        tutorSubjectRepository.save(tutorSubject);
    }

    @Transactional(readOnly = true)
    public List<TutorSearchResponse> searchTutorsBySubject(String subjectTitle) {
        // Relies on the JOIN FETCH query to pull Tutor, Subject, and Rate in one DB hit
        List<TutorSubject> results = tutorSubjectRepository.findActiveTutorSubjectsBySubjectTitle(subjectTitle);

        return results.stream()
                .map(ts -> new TutorSearchResponse(
                        ts.getTutor().getTutorId(),
                        ts.getTutor().getName(),
                        ts.getTutor().getBio(),
                        ts.getTutor().getCountry(),
                        ts.getSubject().getSubjectTitle(),
                        ts.getHourlyRate()
                ))
                .collect(Collectors.toList());
    }
}
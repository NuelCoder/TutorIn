package com.TutorIn.service.REPORT;

import com.TutorIn.dto.ReportRequest;
import com.TutorIn.dto.ReportResponse;
import com.TutorIn.entity.Reports;
import com.TutorIn.entity.Tutor;
import com.TutorIn.entity.User;
import com.TutorIn.enums.ReportStatus;
import com.TutorIn.enums.RoleType;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.ReportRepository;
import com.TutorIn.repository.SessionRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private SessionRepository sessionRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    public void createReport_whenUserReportsTutor_thenSavesSuccessfully() {
        String reporterEmail = "user@tutorin.com";
        ReportRequest request = new ReportRequest("tutor@tutorin.com", RoleType.TUTOR, null, "Bad behavior");

        User mockReporter = new User();
        mockReporter.setEmail(reporterEmail);
        Tutor mockReported = new Tutor();
        mockReported.setEmail("tutor@tutorin.com");

        when(userRepository.findByEmail(reporterEmail)).thenReturn(Optional.of(mockReporter));
        when(tutorRepository.findByEmail("tutor@tutorin.com")).thenReturn(Optional.of(mockReported));

        when(reportRepository.save(any(Reports.class))).thenAnswer(i -> {
            Reports r = i.getArgument(0);
            r.setStatus(ReportStatus.PENDING);
            return r;
        });

        ReportResponse response = reportService.createReport(reporterEmail, false, request);

        assertEquals("PENDING", response.status());
        verify(reportRepository, times(1)).save(any(Reports.class));
    }

    @Test
    public void createReport_whenReportingSelf_thenThrowsBadRequest() {
        String reporterEmail = "user@tutorin.com";
        // User trying to report their own email
        ReportRequest request = new ReportRequest("user@tutorin.com", RoleType.USER, null, "Test");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                reportService.createReport(reporterEmail, false, request));

        assertEquals("You cannot report yourself.", exception.getMessage());
        verify(reportRepository, never()).save(any());
    }
}
package com.TutorIn.service.REPORT;

import com.TutorIn.dto.AdminResolveReportRequest;
import com.TutorIn.dto.ReportRequest;
import com.TutorIn.dto.ReportResponse;
import com.TutorIn.entity.Reports;
import com.TutorIn.entity.Session;
import com.TutorIn.entity.Tutor;
import com.TutorIn.entity.User;
import com.TutorIn.enums.ReportStatus;
import com.TutorIn.enums.RoleType;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.ReportRepository;
import com.TutorIn.repository.SessionRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'TUTOR')")
    public ReportResponse createReport(String reporterEmail, boolean isReporterTutor, ReportRequest request) {
        if (reporterEmail.equalsIgnoreCase(request.reportedEmail())) {
            throw new BadRequestException("You cannot report yourself.");
        }

        Reports report = new Reports();
        report.setReason(request.reason());
        report.setStatus(ReportStatus.PENDING);

        // 1. Setup Reporter (The Accuser)
        if (isReporterTutor) {
            Tutor reporter = tutorRepository.findByEmail(reporterEmail)
                    .orElseThrow(() -> new BadRequestException("Reporter not found"));
            report.setReporterType(RoleType.TUTOR);
            report.setReporterTutor(reporter);
        } else {
            User reporter = userRepository.findByEmail(reporterEmail)
                    .orElseThrow(() -> new BadRequestException("Reporter not found"));
            report.setReporterType(RoleType.USER);
            report.setReporterUser(reporter);
        }

        // 2. Setup Reported (The Accused)
        if (request.reportedType() == RoleType.TUTOR) {
            Tutor reported = tutorRepository.findByEmail(request.reportedEmail())
                    .orElseThrow(() -> new BadRequestException("Reported tutor not found"));
            report.setReportedType(RoleType.TUTOR);
            report.setReportedTutor(reported);
        } else {
            User reported = userRepository.findByEmail(request.reportedEmail())
                    .orElseThrow(() -> new BadRequestException("Reported user not found"));
            report.setReportedType(RoleType.USER);
            report.setReportedUser(reported);
        }

        // 3. Link Contextual Session if provided
        if (request.sessionId() != null) {
            Session session = sessionRepository.findById(request.sessionId())
                    .orElseThrow(() -> new BadRequestException("Session not found"));
            report.setSession(session);
        }

        return mapToResponse(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReportResponse> getReportsByStatus(String status) {
        try {
            ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
            return reportRepository.findByStatus(reportStatus).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid report status. Accepted values: PENDING, IN_REVIEW, RESOLVED, DISMISSED.");
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReportResponse> getReportsAgainstEmail(String email) {
        return reportRepository.findReportsAgainstEmail(email).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReportResponse> getReportsFiledByEmail(String email) {
        return reportRepository.findReportsFiledByEmail(email).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ReportResponse resolveReport(UUID reportId, AdminResolveReportRequest request) {
        Reports report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BadRequestException("Report not found"));

        try {
            report.setStatus(ReportStatus.valueOf(request.status().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status. Accepted values: PENDING, IN_REVIEW, RESOLVED, DISMISSED.");
        }

        report.setAdminResolutionNotes(request.adminResolutionNotes());
        return mapToResponse(reportRepository.save(report));
    }

    private ReportResponse mapToResponse(Reports report) {
        String reporterEmail = report.getReporterType() == RoleType.TUTOR
                ? report.getReporterTutor().getEmail()
                : report.getReporterUser().getEmail();

        String reportedEmail = report.getReportedType() == RoleType.TUTOR
                ? report.getReportedTutor().getEmail()
                : report.getReportedUser().getEmail();

        UUID sessionId = report.getSession() != null ? report.getSession().getSessionId() : null;

        return new ReportResponse(
                report.getReportId(),
                reporterEmail,
                reportedEmail,
                sessionId,
                report.getReason(),
                report.getAdminResolutionNotes(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }
}
package com.TutorIn.service.ADMIN;

import com.TutorIn.dto.*;
import com.TutorIn.entity.*;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.*;
import com.TutorIn.security.RedisSecurityCache;
import com.TutorIn.service.OTP.OtpService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepository;
    private final PendingAdminRepository pendingAdminRepository;
    private final UserRepository userRepository;
    private final PendingUserRepository pendingUserRepository;
    private final TutorRepository tutorRepository;
    private final PendingTutorRepository pendingTutorRepository;
    private final RedisSecurityCache redisSecurityCache;
    private final SystemAuditLogsRepository systemAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;

    @Transactional
    public void verifyOtpAndRegisterUser(OtpVerificationRequest request) {
        long currentAdminCount = adminRepository.count();
        if (currentAdminCount >= 3) {
            throw new BadRequestException("Admin limit reached. System only supports maximum of 3 administrators.");
        }
        if (!otpService.validateAdminRegistrationOTP(request.getOtp(), request.getEmail())) {
            throw new BadRequestException("Invalid or Expired OTP");
        }
        PendingAdmin pendingAdmin = pendingAdminRepository.findByAdminEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Admin with email " + request.getEmail() + " does not exist"));

        Admin admin = new Admin();
        admin.setAdminName(pendingAdmin.getAdminName());
        admin.setAdminEmail(pendingAdmin.getAdminEmail());
        admin.setAdminPassword(pendingAdmin.getAdminPassword());
        admin.setRole(Role.ADMIN);

        adminRepository.save(admin);
        pendingAdminRepository.delete(pendingAdmin);
        otpService.clearOtp(request.getEmail());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Admin getAdminProfile(String adminEmail) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String loggedInEmail = authentication.getName();

        if (!loggedInEmail.equalsIgnoreCase(adminEmail)) {
            throw new AccessDeniedException("You can only view your details");
        }
        return adminRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("Admin does not exist"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteAdminProfile(String adminEmail) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = authentication.getName();
        if (!loggedInEmail.equalsIgnoreCase(adminEmail)) {
            throw new AccessDeniedException("You can only delete your account");
        }
        Admin admin = adminRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("Admin does not exist"));

        admin.setActive(false);
        admin.setDeleted(true);
        adminRepository.save(admin);

        refreshTokenService.revokeAllSessions(adminEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Admin updateAdminProfile(String adminEmail, UpdateAdmin updateAdmin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = authentication.getName();

        if (!loggedInEmail.equalsIgnoreCase(adminEmail)) {
            throw new AccessDeniedException("You can only update your account");
        }

        List<String> forbiddenUpdates = new ArrayList<>();

        Admin admin = adminRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("Admin not found"));

        if (Objects.nonNull(updateAdmin.getAdminName())) {
            admin.setAdminName(updateAdmin.getAdminName());
        }

        return adminRepository.save(admin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void initiateAdminPasswordChange(PasswordChangeRequest passwordChangeRequest) {
        Admin admin = adminRepository.findByAdminEmail(passwordChangeRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Admin does not exist"));

        if (!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), admin.getAdminPassword())) {
            throw new BadRequestException("Incorrect Old Password");
        }

        String otp = otpService.generateForgottenPasswordOTP(admin.getAdminEmail());

        EmailEvent event = new EmailEvent(
                admin.getAdminEmail(),
                "TutorIn: Secure Admin Password Change",
                "Hello " + admin.getAdminName() + ",\n\n" +
                        "You requested to change your password from your account settings. Here is your authorization OTP:\n\n" +
                        "🔑 " + otp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "ADMIN PASSWORD CHANGE"
        );
        kafkaTemplate.send("email-topic", event);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void validateAdminPasswordChange(PasswordVerificationRequest passwordVerificationRequest) {
        if (!otpService.validateAdminPasswordOTP(passwordVerificationRequest.getOtp(), passwordVerificationRequest.getEmail())) {
            throw new BadRequestException("Invalid or Expired OTP");
        }
        Admin admin = adminRepository.findByAdminEmail(passwordVerificationRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Admin does not exist"));

        if (passwordEncoder.matches(passwordVerificationRequest.getNewPassword(), passwordVerificationRequest.getOldPassword())) {
            throw new BadRequestException("This password is already in use");
        }
        admin.setAdminPassword(passwordEncoder.encode(passwordVerificationRequest.getNewPassword()));
        adminRepository.save(admin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void initiateAdminForgotPassword(String email) {
        Admin admin = adminRepository.findByAdminEmail(email)
                .orElseThrow(() -> new BadRequestException("If this email is registered, an OTP will be sent."));

        String otp = otpService.generateAdminForgottenPasswordOTP(admin.getAdminEmail());

        EmailEvent event = new EmailEvent(
                admin.getAdminEmail(),
                "TutorIn: Password Reset Request",
                "Hello " + admin.getAdminName() + ",\n\n" +
                        "You recently requested to reset your password. Here is your One-Time Password (OTP):\n\n" +
                        "🔑 " + otp + "\n\n" +
                        "If you did not make this request, you can safely ignore this email.\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "ADMIN PASSWORD CHANGE"
        );

        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void resetAdminForgottenPassword(PasswordVerificationRequest request) {
        if (!otpService.validateAdminPasswordOTP(request.getOtp(), request.getEmail())) {
            throw new BadRequestException("Invalid or Expired OTP");
        }

        Admin admin = adminRepository.findByAdminEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Admin does not exist"));

        if (passwordEncoder.matches(request.getNewPassword(), admin.getAdminPassword())) {
            throw new BadRequestException("Your new password cannot be the same as your current password");
        }

        admin.setAdminPassword(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);

        // Kicks out any potential hackers by invalidating existing sessions
        refreshTokenService.revokeAllSessions(admin.getAdminEmail());
    }


    public void resendAdminRegistrationOtp(String adminEmail) {
        PendingAdmin pendingAdmin = pendingAdminRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email. Please register first."));

        String newOtp = otpService.refreshAdminRegistrationOTP(adminEmail);

        EmailEvent event = new EmailEvent(
                adminEmail,
                "TutorIn: Your New Registration Code",
                "Hello " + pendingAdmin.getAdminName() + ",\n\n" +
                        "You requested a new verification code. Here is your new One-Time Password (OTP):\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "REGISTRATION"
        );

        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    public void resendAdminForgotPasswordOTP(String adminEmail) {
        Admin admin = adminRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("No admin found for this email"));

        String newOtp = otpService.refreshAdminPasswordOTP(adminEmail);

        EmailEvent event = new EmailEvent(
                adminEmail,
                "TutorIn: Your New Password Reset Code",
                "Hello " + admin.getAdminName() + ",\n\n" +
                        "You requested a new verification code to reset your password. Here is your new One-Time Password (OTP):\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "ADMIN_PASSWORD_RESET"
        );

        kafkaTemplate.send("email-topic", event);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resendAdminPasswordChangeOTP(String adminEmail) {
        Admin admin = adminRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("No admin found for this email"));

        String newOtp = otpService.refreshAdminPasswordOTP(adminEmail);

        EmailEvent event = new EmailEvent(
                adminEmail,
                "TutorIn: Your New Password Change Code",
                "Hello " + admin.getAdminName() + ",\n\n" +
                        "You requested a new verification code to change your password from your account settings. Here is your new One-Time Password (OTP):\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "ADMIN_PASSWORD_CHANGE"
        );

        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    public void resendAdminRecoveryOTP(String adminEmail) {
        Admin admin = adminRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("No admin found for this email."));

        String newOtp = otpService.refreshRecoveryOTP(adminEmail);

        EmailEvent event = new EmailEvent(
                adminEmail,
                "TutorIn: Your New Account Recovery Code",
                "Hello " + admin.getAdminName() + ",\n\n" +
                        "You requested a new verification code to recover your account. Here is your new OTP:\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "RECOVERY"
        );
        kafkaTemplate.send("email-topic", event);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void adminOverrideTutorEmail(String tutorCurrentEmail, String newTutorEmail, String adminEmail) {

        if (tutorRepository.findByEmail(newTutorEmail).isPresent()) {
            throw new BadRequestException("The new email address is already registered to another account.");
        }

        Tutor tutor = tutorRepository.findByEmail(tutorCurrentEmail)
                .orElseThrow(() -> new BadRequestException("Tutor with the current email not found."));

        tutor.setEmail(newTutorEmail);
        tutorRepository.save(tutor);

        String targetDetails = "Changed " + tutorCurrentEmail + " to " + newTutorEmail;
        logAdminAction(adminEmail, "CHANGED_TUTOR_EMAIL", targetDetails);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void adminOverrideUserEmail(String userCurrentEmail, String newUserEmail, String adminEmail) {

        if (userRepository.findByEmail(newUserEmail).isPresent()) {
            throw new BadRequestException("The new email address is already registered to another account.");
        }

        User user = userRepository.findByEmail(userCurrentEmail)
                .orElseThrow(() -> new BadRequestException("User with the current email not found."));

        user.setEmail(newUserEmail);
        userRepository.save(user);

        String targetDetails = "Changed " + userCurrentEmail + " to " + newUserEmail;
        logAdminAction(adminEmail, "CHANGED_USER_EMAIL", targetDetails);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String adminGenerateManualOtpForUserRegistration(String userEmail, String adminEmail) {
        // 1. Verify the user actually exists
        PendingUser pendingUser = pendingUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BadRequestException("User not found. Cannot generate OTP."));

        logAdminAction(adminEmail, "USER_REGISTRATION_OTP", userEmail);

        // 2. Return the OTP string so the Admin can read it out to the user manually
        return otpService.generateRegistrationOTP(userEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String adminGenerateManualOtpForTutorRegistration(String tutorEmail, String adminEmail) {
        // 1. Verify the user actually exists
        PendingTutor pendingTutor = pendingTutorRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new BadRequestException("Tutor not found. Cannot generate OTP."));

        logAdminAction(adminEmail, "TUTOR_REGISTRATION_OTP", tutorEmail);

        // 2. Return the OTP string so the Admin can read it out to the user manually
        return otpService.generateTutorRegistrationOTP(tutorEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String adminGenerateManualOtpForUserForgottenPass(String userEmail, String adminEmail) {
        // 1. Verify the user actually exists
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BadRequestException("User not found. Cannot generate OTP."));

        logAdminAction(adminEmail, "FORGOTTEN_PASSWORD_OTP", userEmail);

        // 2. Return the OTP string so the Admin can read it out to the user manually
        return otpService.generateForgottenPasswordOTP(userEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String adminGenerateManualOtpForTutorForgottenPass(String tutorEmail, String adminEmail) {
        // 1. Verify the user actually exists
        Tutor tutor = tutorRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new BadRequestException("Tutor not found. Cannot generate OTP."));

        logAdminAction(adminEmail, "FORGOTTEN_PASSWORD_OTP", tutorEmail);

        // 2. Return the OTP string so the Admin can read it out to the user manually
        return otpService.generateTutorForgottenPasswordOTP(tutorEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String adminGenerateManualOtpForUserPasswordChange(String userEmail, String adminEmail) {
        // 1. Verify the user actually exists
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BadRequestException("User not found. Cannot generate OTP."));

        logAdminAction(adminEmail, "PASSWORD_CHANGE_OTP", userEmail);
        // 2. Return the OTP string so the Admin can read it out to the user manually
        return otpService.generateForgottenPasswordOTP(userEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String adminGenerateManualOtpForTutorPasswordChange(String tutorEmail, String adminEmail) {
        // 1. Verify the user actually exists
        Tutor tutor = tutorRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new BadRequestException("Tutor not found. Cannot generate OTP."));

        logAdminAction(adminEmail, "PASSWORD_CHANGE_OTP", tutorEmail);
        // 2. Return the OTP string so the Admin can read it out to the user manually
        return otpService.generateTutorForgottenPasswordOTP(tutorEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void toggleUserSuspension(String userEmail, boolean suspend, String adminEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.isSuspended() == suspend) {
            String currentState = suspend ? "suspended" : "active";
            throw new BadRequestException("Action rejected: User account is already " + currentState + ".");
        }

        user.setSuspended(suspend);
        user.setAccountNonLocked(!suspend);
        userRepository.save(user);

        if (suspend) {
            refreshTokenService.revokeAllSessions(userEmail);
            redisSecurityCache.blockUser(userEmail);
            redisSecurityCache.setTokenCutoff(userEmail);
        }
        else {
            redisSecurityCache.unblockUser(userEmail);
        }

        String action = suspend ? "SUSPENDED_USER" : "REACTIVATED_USER";
        logAdminAction(adminEmail, action, userEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void toggleTutorSuspension(String tutorEmail, boolean suspend, String adminEmail) {
        Tutor tutor = tutorRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new BadRequestException("Tutor not found"));

        if (tutor.isSuspended() == suspend) {
            String currentState = suspend ? "suspended" : "active";
            throw new BadRequestException("Action rejected: Tutor account is already " + currentState + ".");
        }

        tutor.setSuspended(suspend);
        tutor.setAccountNonLocked(!suspend);
        tutorRepository.save(tutor);

        if (suspend) {
            refreshTokenService.revokeAllSessions(tutorEmail);
            redisSecurityCache.blockUser(tutorEmail);
            redisSecurityCache.setTokenCutoff(tutorEmail);
        }
        else {
            redisSecurityCache.unblockUser(tutorEmail);
        }

        String action = suspend ? "SUSPENDED_TUTOR" : "REACTIVATED_TUTOR";
        logAdminAction(adminEmail, action, tutorEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<SystemAuditLogs> getRecentAuditLogs() {
        return systemAuditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    public void logAdminAction(String adminEmail, String action, String target) {
        SystemAuditLogs log = SystemAuditLogs.builder()
                .adminEmail(adminEmail)
                .action(action)
                .target(target)
                .build();
        systemAuditLogRepository.save(log);
    }
}

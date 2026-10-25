package com.TutorIn.service.TUTOR;

import com.TutorIn.dto.*;
import com.TutorIn.entity.PendingTutor;
import com.TutorIn.entity.Tutor;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.AccessDeniedException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.PendingTutorRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.service.OTP.OtpService;
import com.TutorIn.service.REFRESHTOKEN.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
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
public class TutorService {
    private final TutorRepository tutorRepository;
    private final PendingTutorRepository pendingTutorRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;


    @Transactional
    public void verifyOtpAndRegisterUser(OtpVerificationRequest otpVerificationRequest){
        if (!otpService.validateTutorRegistrationOTP(otpVerificationRequest.getOtp(), otpVerificationRequest.getEmail())){
            throw new BadRequestException("Invalid or Expired OTP");
        }

        PendingTutor pendingTutor = pendingTutorRepository.findByEmail(otpVerificationRequest.getEmail())
                .orElseThrow(()-> new BadRequestException("Tutor with email " + otpVerificationRequest.getEmail() + " does not exist"));

        Tutor tutor = new Tutor();
        tutor.setName(pendingTutor.getName());
        tutor.setEmail(pendingTutor.getEmail());
        tutor.setBio(pendingTutor.getBio());
        tutor.setGender(pendingTutor.getGender());
        tutor.setPassword(pendingTutor.getPassword());
        tutor.setCountry(pendingTutor.getCountry());
        tutor.setRole(Role.TUTOR);

        tutorRepository.save(tutor);
        EmailEvent event = new EmailEvent(
                otpVerificationRequest.getEmail(),
                "Account Verification",
                "Dear tutor,\n\nYour account has been verified. and saved\n\n- TutorIn",
                "VERIFICATION"
        );
        kafkaTemplate.send("email-topic", event);
        pendingTutorRepository.delete(pendingTutor);
        otpService.clearOtp(otpVerificationRequest.getEmail());
    }

    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public Tutor getTutorProfile(String email){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        String loggedInEmail = authentication.getName();
        if (!isAdmin && !loggedInEmail.equalsIgnoreCase(email)){
            throw new AccessDeniedException("You can only view your own account");
        }
        return tutorRepository.findByEmail(email)
                .orElseThrow(()-> new BadRequestException("Tutor does not exist"));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public void deleteTutorProfile(String email){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        String loggedInEmail = authentication.getName();
        if (!isAdmin && !loggedInEmail.equalsIgnoreCase(email)){
            throw new AccessDeniedException("You can only delete your account");
        }
        Tutor tutor = tutorRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Tutor does not exist"));

        tutor.setActive(false);
        tutor.setDeleted(true);
        tutorRepository.save(tutor);

        refreshTokenService.revokeAllSessions(email);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public Tutor updateTutorProfile(String email, UpdateTutor updateTutor){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        String loggedInEmail = authentication.getName();

        Tutor tutor = tutorRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Tutor does not exist"));

        if (!isAdmin && !loggedInEmail.equalsIgnoreCase(email)){
            throw new AccessDeniedException("You can only update your account's details");
        }

        List<String> forbiddenUpdates = new ArrayList<>();

        if (Objects.nonNull(updateTutor.getName())){
            tutor.setName(updateTutor.getName());
        }

        if (Objects.nonNull(updateTutor.getEmail())){
            if (!isAdmin){
                forbiddenUpdates.add("email");
            }else {
                refreshTokenService.revokeAllSessions(email);
                tutor.setEmail(updateTutor.getEmail());
            }
        }
        if (Objects.nonNull(updateTutor.getBio())){
            tutor.setBio(updateTutor.getBio());
        }
        if (Objects.nonNull(updateTutor.getGender())){
            tutor.setGender(updateTutor.getGender());
        }
        if (Objects.nonNull(updateTutor.getCountry())){
            tutor.setCountry(updateTutor.getCountry());
        }
        if (!forbiddenUpdates.isEmpty()) {
            throw new AccessDeniedException(
                    "You are not allowed to update these fields directly: " + String.join(", ", forbiddenUpdates)
            );
        }

        return tutorRepository.save(tutor);
    }

    @PreAuthorize("hasRole('TUTOR')")
    public void initiatePasswordChange(PasswordChangeRequest passwordChangeRequest){
        Tutor tutor = tutorRepository.findByEmail(passwordChangeRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Tutor does not exist"));

        if (!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), tutor.getPassword())){
            throw new BadRequestException("Incorrect Old Password");
        }

        String otp = otpService.generateTutorForgottenPasswordOTP(tutor.getEmail());

        EmailEvent event = new EmailEvent(
                tutor.getEmail(),
                "TutorIn: Secure Password Change",
                "Hello " + tutor.getName() + ",\n\n" +
                        "You requested to change your password from your account settings. Here is your authorization OTP:\n\n" +
                        "🔑 " + otp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "PASSWORD CHANGE"
        );

        kafkaTemplate.send("email-topic", event);
    }

    @PreAuthorize("hasRole('TUTOR')")
    @Transactional
    public void validatePasswordChange(PasswordVerificationRequest passwordVerificationRequest){
        if (!otpService.validateTutorPasswordOTP(passwordVerificationRequest.getOtp(), passwordVerificationRequest.getEmail())){
            throw new BadRequestException("Invalid or Expired OTP");
        }
        Tutor tutor = tutorRepository.findByEmail(passwordVerificationRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Tutor does not exist"));

        if (passwordEncoder.matches(passwordVerificationRequest.getNewPassword(), tutor.getPassword())){
            throw new BadRequestException("This password is already in use");
        }
        tutor.setPassword(passwordEncoder.encode(passwordVerificationRequest.getNewPassword()));
        tutorRepository.save(tutor);
    }

    public void initiateForgotPassword(String email) {
        Tutor tutor = tutorRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Tutor does not exist"));

        String otp = otpService.generateTutorForgottenPasswordOTP(tutor.getEmail());

        EmailEvent event = new EmailEvent(
                tutor.getEmail(),
                "TutorIn: Password Reset Request",
                "Hello " + tutor.getName() + ",\n\n" +
                        "You recently requested to reset your password. Here is your One-Time Password (OTP):\n\n" +
                        "🔑 " + otp + "\n\n" +
                        "If you did not make this request, you can safely ignore this email.\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "PASSWORD CHANGE"
        );

        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    public void resetForgottenPassword(PasswordVerificationRequest request) {
        if (!otpService.validateTutorPasswordOTP(request.getOtp(), request.getEmail())){
            throw new BadRequestException("Invalid or Expired OTP");
        }

        Tutor tutor = tutorRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Tutor does not exist"));

        if (passwordEncoder.matches(request.getNewPassword(), tutor.getPassword())){
            throw new BadRequestException("Your new password cannot be the same as your current password");
        }

        tutor.setPassword(passwordEncoder.encode(request.getNewPassword()));
        tutorRepository.save(tutor);

        // Kicks out any potential hackers by invalidating existing sessions
        refreshTokenService.revokeAllSessions(tutor.getEmail());
    }

    @Transactional
    public void resendRegistrationOtp(String email) {

        PendingTutor pendingTutor = pendingTutorRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email. Please register first."));

        String newOtp = otpService.refreshTutorRegistrationOTP(email);

        EmailEvent event = new EmailEvent(
                email,
                "TutorIn: Your New Registration Code",
                "Hello " + pendingTutor.getName() + ",\n\n" +
                        "You requested a new verification code. Here is your new One-Time Password (OTP):\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "REGISTRATION"
        );

        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    public void resendForgotPasswordOTP(String email) {
        Tutor tutor = tutorRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No tutor found for this email."));

        String newOtp = otpService.refreshTutorPasswordOTP(email);

        EmailEvent event = new EmailEvent(
                email,
                "TutorIn: Your New Password Reset Code",
                "Hello " + tutor.getName() + ",\n\n" +
                        "You requested a new verification code to reset your password. Here is your new OTP:\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "PASSWORD_RESET"
        );
        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    @PreAuthorize("hasRole('TUTOR')")
    public void resendPasswordChangeOTP(String email) {
        Tutor tutor = tutorRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No tutor found for this email."));

        String newOtp = otpService.refreshTutorPasswordOTP(email);

        EmailEvent event = new EmailEvent(
                email,
                "TutorIn: Your New Password Change Code",
                "Hello " + tutor.getName() + ",\n\n" +
                        "You requested a new verification code to change your password from your account settings. Here is your new OTP:\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "PASSWORD_CHANGE"
        );
        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    public void resendRecoveryOTP(String email) {
        Tutor tutor = tutorRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No tutor found for this email."));

        String newOtp = otpService.refreshRecoveryOTP(email);

        EmailEvent event = new EmailEvent(
                email,
                "TutorIn: Your New Account Recovery Code",
                "Hello " + tutor.getName() + ",\n\n" +
                        "You requested a new verification code to recover your account. Here is your new OTP:\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "RECOVERY"
        );
        kafkaTemplate.send("email-topic", event);
    }
}

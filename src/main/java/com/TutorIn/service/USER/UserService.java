package com.TutorIn.service.USER;

import com.TutorIn.dto.*;
import com.TutorIn.entity.PendingUser;
import com.TutorIn.entity.User;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.AccessDeniedException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.PendingUserRepository;
import com.TutorIn.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PendingUserRepository pendingUserRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;

    @Transactional
    public void verifyOtpAndRegisterUser(OtpVerificationRequest otpVerificationRequest){
        if (!otpService.validateRegistrationOTP(otpVerificationRequest.getOtp(), otpVerificationRequest.getEmail())){
            throw new BadRequestException("Invalid or Expired OTP");
        }

        PendingUser pendingUser = pendingUserRepository.findByEmail(otpVerificationRequest.getEmail())
                .orElseThrow(()-> new BadRequestException("User with email " + otpVerificationRequest.getEmail() + " does not exist"));

        User user = new User();
        user.setName(pendingUser.getName());
        user.setEmail(pendingUser.getEmail());
        user.setGender(pendingUser.getGender());
        user.setPassword(pendingUser.getPassword());
        user.setCountry(pendingUser.getCountry());
        user.setRole(Role.USER);

        userRepository.save(user);
        EmailEvent event = new EmailEvent(
                otpVerificationRequest.getEmail(),
                "Account Verification",
                "Dear user,\n\nYour account has been verified. and saved\n\n- TutorIn",
                "VERIFICATION"
        );
        kafkaTemplate.send("email-topic", event);
        pendingUserRepository.delete(pendingUser);
        otpService.clearOtp(otpVerificationRequest.getEmail());


    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public User getUserProfile(String email){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        String loggedInEmail = authentication.getName();
        if (!isAdmin && !loggedInEmail.equalsIgnoreCase(email)){
            throw new AccessDeniedException("You can only view your own account");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new BadRequestException("User does not exist"));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void deleteUserProfile(String email){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        String loggedInEmail = authentication.getName();
        if (!isAdmin && !loggedInEmail.equalsIgnoreCase(email)){
            throw new AccessDeniedException("You can only delete your account");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        user.setActive(false);
        user.setDeleted(true);
        userRepository.save(user);

        refreshTokenService.revokeAllSessions(email);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public User updateUserProfile(String email, UpdateUser updateUser){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        String loggedInEmail = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        if (!isAdmin && !loggedInEmail.equalsIgnoreCase(email)){
            throw new AccessDeniedException("You can only update your account's details");
        }

        List<String> forbiddenUpdates = new ArrayList<>();

        if (Objects.nonNull(updateUser.getName())){
            user.setName(updateUser.getName());
        }

        if (Objects.nonNull(updateUser.getEmail())){
            if (!isAdmin){
                forbiddenUpdates.add("email");
            }else {
                refreshTokenService.revokeAllSessions(email);
                user.setEmail(updateUser.getEmail());
            }
        }
        if (Objects.nonNull(updateUser.getGender())){
            user.setGender(updateUser.getGender());
        }
        if (Objects.nonNull(updateUser.getCountry())){
            user.setCountry(updateUser.getCountry());
        }
        if (!forbiddenUpdates.isEmpty()) {
            throw new AccessDeniedException(
                    "You are not allowed to update these fields directly: " + String.join(", ", forbiddenUpdates)
            );
        }

        return userRepository.save(user);
    }

    @PreAuthorize("hasRole('USER')")
    public void initiatePasswordChange(PasswordChangeRequest passwordChangeRequest){
        User user = userRepository.findByEmail(passwordChangeRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        if (!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), user.getPassword())){
            throw new BadRequestException("Incorrect Old Password");
        }

        String otp = otpService.generateForgottenPasswordOTP(user.getEmail());

        EmailEvent event = new EmailEvent(
                user.getEmail(),
                "TutorIn: Secure Password Change",
                "Hello " + user.getName() + ",\n\n" +
                        "You requested to change your password from your account settings. Here is your authorization OTP:\n\n" +
                        "🔑 " + otp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "PASSWORD CHANGE"
        );

        kafkaTemplate.send("email-topic", event);
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public void validatePasswordChange(PasswordVerificationRequest passwordVerificationRequest){
        if (!otpService.validatePasswordOTP(passwordVerificationRequest.getOtp(), passwordVerificationRequest.getEmail())){
            throw new BadRequestException("Invalid or Expired OTP");
        }
        User user = userRepository.findByEmail(passwordVerificationRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        if (passwordEncoder.matches(passwordVerificationRequest.getNewPassword(), user.getPassword())){
            throw new BadRequestException("This password is already in use");
        }
        user.setPassword(passwordEncoder.encode(passwordVerificationRequest.getNewPassword()));
        userRepository.save(user);
    }

    public void initiateForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("If this email is registered, an OTP will be sent."));

        String otp = otpService.generateForgottenPasswordOTP(user.getEmail());

        EmailEvent event = new EmailEvent(
                user.getEmail(),
                "TutorIn: Password Reset Request",
                "Hello " + user.getName() + ",\n\n" +
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
        if (!otpService.validatePasswordOTP(request.getOtp(), request.getEmail())){
            throw new BadRequestException("Invalid or Expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())){
            throw new BadRequestException("Your new password cannot be the same as your current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Kicks out any potential hackers by invalidating existing sessions
        refreshTokenService.revokeAllSessions(user.getEmail());
    }

    @Transactional
    public void resendRegistrationOtp(String email) {

        PendingUser pendingUser = pendingUserRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email. Please register first."));

        String newOtp = otpService.refreshRegistrationOTP(email);

        EmailEvent event = new EmailEvent(
                email,
                "TutorIn: Your New Registration Code",
                "Hello " + pendingUser.getName() + ",\n\n" +
                        "You requested a new verification code. Here is your new One-Time Password (OTP):\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "REGISTRATION"
        );

        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    public void resendForgotPasswordOTP(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No user found for this email."));

        String newOtp = otpService.refreshPasswordOTP(email);

        EmailEvent event = new EmailEvent(
                email,
                "TutorIn: Your New Password Reset Code",
                "Hello " + user.getName() + ",\n\n" +
                        "You requested a new verification code to reset your password. Here is your new OTP:\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "PASSWORD_RESET"
        );
        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void resendPasswordChangeOTP(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No user found for this email."));

        String newOtp = otpService.refreshPasswordOTP(email);

        EmailEvent event = new EmailEvent(
                email,
                "TutorIn: Your New Password Change Code",
                "Hello " + user.getName() + ",\n\n" +
                        "You requested a new verification code to change your password from your account settings. Here is your new OTP:\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "PASSWORD_CHANGE"
        );
        kafkaTemplate.send("email-topic", event);
    }

    @Transactional
    public void resendRecoveryOTP(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No user found for this email."));

        String newOtp = otpService.refreshRecoveryOTP(email);

        EmailEvent event = new EmailEvent(
                email,
                "TutorIn: Your New Account Recovery Code",
                "Hello " + user.getName() + ",\n\n" +
                        "You requested a new verification code to recover your account. Here is your new OTP:\n\n" +
                        "🔑 " + newOtp + "\n\n" +
                        "Warmly,\nThe TutorIn Team",
                "RECOVERY"
        );
        kafkaTemplate.send("email-topic", event);
    }
}

package com.TutorIn.service.PENDINGADMIN;


import com.TutorIn.dto.EmailEvent;
import com.TutorIn.dto.PendingAdminDTO;
import com.TutorIn.entity.PendingAdmin;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.BadInputException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.AdminRepository;
import com.TutorIn.repository.PendingAdminRepository;
import com.TutorIn.service.OTP.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PendingAdminService {
    private final PendingAdminRepository pendingAdminRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;


    public PendingAdmin registerAdmin(PendingAdminDTO pendingAdminDTO){
        if (pendingAdminRepository.findByAdminEmail(pendingAdminDTO.getAdminEmail()).isPresent()){
            throw new BadRequestException("Pending admin with this email:" + pendingAdminDTO.getAdminEmail() + "already exists");
        }

        if (adminRepository.existsByAdminEmail(pendingAdminDTO.getAdminEmail())){
            throw new BadRequestException("Admin with this email:" + pendingAdminDTO.getAdminEmail() + "already exists");
        }

        validateInput(pendingAdminDTO);

        PendingAdmin pendingAdmin = new PendingAdmin();
        pendingAdmin.setAdminName(pendingAdminDTO.getAdminName());
        pendingAdmin.setAdminEmail(pendingAdminDTO.getAdminEmail());
        pendingAdmin.setAdminPassword(passwordEncoder.encode(pendingAdminDTO.getAdminPassword()));
        pendingAdmin.setRole(Role.PENDING);

        PendingAdmin savedAdmin = pendingAdminRepository.save(pendingAdmin);

        String otp = otpService.generateAdminRegistrationOTP(pendingAdminDTO.getAdminEmail());
        EmailEvent event = new EmailEvent(
                pendingAdminDTO.getAdminEmail(),
                "Welcome to EarsForYou - Verification Code",
                "Your OTP for registration is: " + otp,
                "REGISTRATION"
        );
        kafkaTemplate.send("email-topic", event);

        return savedAdmin;
    }


    private void validateInput(PendingAdminDTO pendingAdminDTO){
        if (!isValidPassword(pendingAdminDTO.getAdminPassword()))throw new BadInputException("Invalid Password");
        if (!isValidEmail(pendingAdminDTO.getAdminEmail()))throw new BadInputException("Invalid Email");
    }

    private boolean isValidEmail(String email){
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Objects.nonNull(email) && email.matches(emailRegex);
    }

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.\\-_#])[A-Za-z0-9@$!%*&?.\\-_#]{8,}$"
    );

    private boolean isValidPassword(String password) {
        return Objects.nonNull(password) && PASSWORD_PATTERN.matcher(password).matches();
    }
}


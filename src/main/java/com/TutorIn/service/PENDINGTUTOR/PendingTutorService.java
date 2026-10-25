package com.TutorIn.service.PENDINGTUTOR;

import com.TutorIn.dto.EmailEvent;
import com.TutorIn.dto.PendingTutorDTO;
import com.TutorIn.dto.PendingUserDTO;
import com.TutorIn.entity.PendingTutor;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.BadInputException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.PendingTutorRepository;
import com.TutorIn.repository.TutorRepository;
import com.TutorIn.service.OTP.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PendingTutorService {
    private final PendingTutorRepository pendingTutorRepository;
    private final TutorRepository tutorRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;

    public PendingTutor registerTutor(PendingTutorDTO pendingTutorDTO){
        if (tutorRepository.existsByEmail(pendingTutorDTO.getEmail())) {
            throw new BadRequestException("Tutor with this email: " + pendingTutorDTO.getEmail() + " already exists");
        }
        if (pendingTutorRepository.existsByEmail(pendingTutorDTO.getEmail())) {
            throw new BadRequestException("Pending Tutor with this email: " + pendingTutorDTO.getEmail() + " already exists");
        }
        validateInput(pendingTutorDTO);
       PendingTutor pendingTutor = new PendingTutor();
       pendingTutor.setName(pendingTutorDTO.getName());
       pendingTutor.setBio(pendingTutorDTO.getBio());
       pendingTutor.setEmail(pendingTutorDTO.getEmail());
       pendingTutor.setGender(pendingTutorDTO.getGender());
       pendingTutor.setPassword(passwordEncoder.encode(pendingTutorDTO.getPassword()));
       pendingTutor.setCountry(pendingTutorDTO.getCountry());
       pendingTutor.setRole(Role.PENDING);

       PendingTutor savedTutor = pendingTutorRepository.save(pendingTutor);
       String otp = otpService.generateTutorRegistrationOTP(pendingTutorDTO.getEmail());

        EmailEvent event = new EmailEvent(
                pendingTutorDTO.getEmail(),
                "Welcome to TutorIn - Verification Code",
                "Your OTP for registration is: " + otp,
                "REGISTRATION"
        );
        kafkaTemplate.send("email-topic", event);

        return savedTutor;
    }

    private void validateInput(PendingTutorDTO pendingTutorDTO){
        if (!isValidPassword(pendingTutorDTO.getPassword()))throw new BadInputException("Invalid Password");
        if (!isValidEmail(pendingTutorDTO.getEmail()))throw new BadInputException("Invalid Email");
        if (!isValidGender(pendingTutorDTO.getGender()))throw new BadInputException("Invalid Gender");
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

    private boolean isValidGender(String gender){
        return gender != null && gender.matches("(?i)Male|Female|Other|M|F|O");
    }
}

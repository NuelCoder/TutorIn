package com.TutorIn.service.PENDINGUSER;

import com.TutorIn.dto.EmailEvent;
import com.TutorIn.dto.PendingUserDTO;
import com.TutorIn.entity.PendingUser;
import com.TutorIn.entity.User;
import com.TutorIn.enums.Role;
import com.TutorIn.exceptions.BadInputException;
import com.TutorIn.exceptions.BadRequestException;
import com.TutorIn.repository.PendingUserRepository;
import com.TutorIn.repository.UserRepository;
import com.TutorIn.service.OTP.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PendingUserService {
    private final PendingUserRepository pendingUserRepository;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;


    @Transactional
    public PendingUser registerUser(PendingUserDTO pendingUserDTO){
        if (userRepository.existsByEmail(pendingUserDTO.getEmail())) {
            throw new BadRequestException("User with this email: " + pendingUserDTO.getEmail() + " already exists");
        }
        
        if(pendingUserRepository.existsByEmail(pendingUserDTO.getEmail())) {
            throw new BadRequestException("Pending user with this email: " + pendingUserDTO.getEmail() + " already exists");
        }

        validateInput(pendingUserDTO);
        PendingUser pendingUser = new PendingUser();
        pendingUser.setName(pendingUserDTO.getName());
        pendingUser.setEmail(pendingUserDTO.getEmail());
        pendingUser.setPassword(passwordEncoder.encode(pendingUserDTO.getPassword()));
        pendingUser.setGender(pendingUserDTO.getGender());
        pendingUser.setCountry(pendingUserDTO.getCountry());
        pendingUser.setRole(Role.PENDING);

       PendingUser savedUser = pendingUserRepository.save(pendingUser);

       String otp = otpService.generateRegistrationOTP(pendingUserDTO.getEmail());
        EmailEvent event = new EmailEvent(
                pendingUserDTO.getEmail(),
                "Welcome to TutorIn - Verification Code",
                "Your OTP for registration is: " + otp,
                "REGISTRATION"
        );
        kafkaTemplate.send("email-topic", event);

        return savedUser;
    }



    private void validateInput(PendingUserDTO pendingUserDTO){
        if (!isValidPassword(pendingUserDTO.getPassword()))throw new BadInputException("Invalid Password");
        if (!isValidEmail(pendingUserDTO.getEmail()))throw new BadInputException("Invalid Email");
        if (!isValidGender(pendingUserDTO.getGender()))throw new BadInputException("Invalid Gender");
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

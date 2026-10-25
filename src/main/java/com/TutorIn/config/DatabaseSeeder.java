package com.TutorIn.config;

import com.TutorIn.entity.*;
import com.TutorIn.enums.Role;
import com.TutorIn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final TutorRepository tutorRepository;
    private final SubjectRepository subjectRepository;
    private final TutorSubjectRepository tutorSubjectRepository;
    private final AvailabilitySlotRepository slotRepository;

    // Inject the new repositories and password encoder
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_SEED_PASSWORD}")
    private String rawPassword;

    @Override
    public void run(String... args) throws Exception {
        // Only run if the database is completely empty
        if (tutorRepository.count() == 0 && userRepository.count() == 0 && adminRepository.count() == 0) {

            String encodedPassword = passwordEncoder.encode(rawPassword);

            // ==========================================
            // 1. SEED ADMIN
            // ==========================================
            Admin admin = Admin.builder()
                    .adminName("System Admin")
                    .adminEmail("admin@tutorin.com")
                    .adminPassword(encodedPassword)
                    .role(Role.ADMIN)
                    .build();
            adminRepository.save(admin);


            // ==========================================
            // 2. SEED USER (STUDENT)
            // ==========================================
            User student = User.builder()
                    .name("Jane Student")
                    .email("student@tutorin.com")
                    .password(encodedPassword)
                    .country("Nigeria")
                    .gender("Female")
                    .role(Role.USER)
                    .build();
            userRepository.save(student);


            // ==========================================
            // 3. SEED SUBJECTS
            // ==========================================
            Subject math = new Subject(null, "Calculus 101", "Advanced university mathematics");
            Subject physics = new Subject(null, "Physics 101", "Introductory mechanics");
            subjectRepository.save(math);
            subjectRepository.save(physics);


            // ==========================================
            // 4. SEED TUTOR
            // ==========================================
            Tutor john = Tutor.builder()
                    .name("John Doe")
                    .email("tutor@tutorin.com")
                    .gender("Male")
                    .password(encodedPassword)
                    .country("Nigeria")
                    .gender("Male")
                    .bio("Expert Math Tutor")
                    .role(Role.TUTOR)
                    .build();
            tutorRepository.save(john);


            // ==========================================
            // 5. LINK TUTOR TO SUBJECT
            // ==========================================
            TutorSubject johnMath = new TutorSubject();
            johnMath.setTutor(john);
            johnMath.setSubject(math);
            johnMath.setHourlyRate(new BigDecimal("5000.00"));
            johnMath.getId().setTutorId(john.getTutorId());
            johnMath.getId().setSubjectId(math.getSubjectId());
            tutorSubjectRepository.save(johnMath);


            // ==========================================
            // 6. SEED AVAILABILITY SLOTS
            // ==========================================
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

            AvailabilitySlot slot1 = new AvailabilitySlot();
            slot1.setTutor(john);
            slot1.setStartTime(tomorrow.withHour(10).withMinute(0).withSecond(0));
            slot1.setEndTime(tomorrow.withHour(11).withMinute(0).withSecond(0));

            AvailabilitySlot slot2 = new AvailabilitySlot();
            slot2.setTutor(john);
            slot2.setStartTime(tomorrow.withHour(14).withMinute(0).withSecond(0));
            slot2.setEndTime(tomorrow.withHour(15).withMinute(30).withSecond(0));

            slotRepository.save(slot1);
            slotRepository.save(slot2);

            System.out.println("✅ Database Seeding Complete!");
            System.out.println("--- Test Accounts (Password: " + rawPassword + ") ---");
            System.out.println("Admin:   " + admin.getAdminEmail());
            System.out.println("Tutor:   " + john.getEmail() + " | UUID: " + john.getTutorId());
            System.out.println("Student: " + student.getEmail() + " | UUID: " + student.getUserId());
        }
    }
}
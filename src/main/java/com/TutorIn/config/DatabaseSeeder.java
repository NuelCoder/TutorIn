package com.TutorIn.config;

import com.TutorIn.entity.*;
import com.TutorIn.enums.Role;
import com.TutorIn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
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

    @Override
    public void run(String... args) throws Exception {
        if (tutorRepository.count() == 0) {

            // 1. Seed Subjects
            Subject math = new Subject(null, "Calculus 101", "Advanced university mathematics");
            Subject physics = new Subject(null, "Physics 101", "Introductory mechanics");
            subjectRepository.save(math);
            subjectRepository.save(physics);

            // 2. Seed Tutors
            Tutor john = Tutor.builder()
                    .name("John Doe")
                    .email("john@tutorin.com")
                    .password("Password123!") // Will be hashed when Auth is implemented
                    .country("Nigeria")
                    .bio("Expert Math Tutor")
                    .role(Role.TUTOR)
                    .active(true)
                    .build();

            tutorRepository.save(john);

            // 3. Link Tutor to Subject
            TutorSubject johnMath = new TutorSubject();
            johnMath.setTutor(john);
            johnMath.setSubject(math);
            johnMath.setHourlyRate(new BigDecimal("5000.00"));
            johnMath.getId().setTutorId(john.getTutorId());
            johnMath.getId().setSubjectId(math.getSubjectId());
            tutorSubjectRepository.save(johnMath);

            // 4. Seed Slots (Tomorrow at 10 AM and 2 PM)
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

            System.out.println("✅ Seeding Complete. Copy this Tutor UUID to test your APIs: " + john.getTutorId());
        }
    }
}
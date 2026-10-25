package com.TutorIn.service.EMAIL;

import com.TutorIn.dto.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConsumerService {

    private final EmailSenderService emailSenderService;

    @KafkaListener(
            topics = "email-topic",
            groupId = "tutorin-email-group"
    )
    public void consumeEmailEvent(EmailEvent event) {
        log.info("Consuming EmailEvent for: {} | Type: {}", event.getTo(), event.getType());

        emailSenderService.sendEmail(
                event.getTo(),
                event.getSubject(),
                event.getBody()
        );
    }
}
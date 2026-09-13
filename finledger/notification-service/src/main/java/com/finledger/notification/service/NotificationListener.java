package com.finledger.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finledger.notification.dto.MoneySentEvent;
import com.finledger.notification.entity.Notification;
import com.finledger.notification.repository.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationListener {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    public NotificationListener(ObjectMapper objectMapper, NotificationRepository notificationRepository) {
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
    }

    @KafkaListener(topics = "money.sent", groupId = "notification-service")
    public void onMoneySent(String payload) throws Exception {
        MoneySentEvent event = objectMapper.readValue(payload, MoneySentEvent.class);
        saveIfMissing(event, event.toAccount(), "Money received from " + event.fromAccount());
        saveIfMissing(event, event.fromAccount(), "Money sent to " + event.toAccount());
    }

    private void saveIfMissing(MoneySentEvent event, String accountNumber, String message) {
        if (!notificationRepository.existsByTransferIdAndAccountNumberAndMessage(
                event.transferId(), accountNumber, message)) {
            notificationRepository.save(new Notification(
                    event.transferId(), accountNumber, message, event.amount()));
        }
    }
}

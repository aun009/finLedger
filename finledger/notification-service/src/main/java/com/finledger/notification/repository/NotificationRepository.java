package com.finledger.notification.repository;

import com.finledger.notification.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends MongoRepository<Notification, UUID> {

    List<Notification> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    boolean existsByTransferIdAndAccountNumberAndMessage(UUID transferId, String accountNumber, String message);
}

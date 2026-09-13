package com.finledger.notification.controller;

import com.finledger.notification.entity.Notification;
import com.finledger.notification.repository.NotificationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/{accountNumber}")
    public List<Notification> findForAccount(@PathVariable String accountNumber) {
        return notificationRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }
}

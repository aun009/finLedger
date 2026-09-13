package com.finledger.notification.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document("notifications")
public class Notification {

    @Id
    private UUID id;
    private UUID transferId;
    private String accountNumber;
    private String message;
    private BigDecimal amount;
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(UUID transferId, String accountNumber, String message, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.transferId = transferId;
        this.accountNumber = accountNumber;
        this.message = message;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTransferId() { return transferId; }
    public String getAccountNumber() { return accountNumber; }
    public String getMessage() { return message; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.finledger.account.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A durable authorisation hold, analogous to a card or UPI collect-payment hold. */
@Entity
@Table(name = "balance_holds")
public class BalanceHold {
    public enum Status { ACTIVE, SETTLED, RELEASED }

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 100)
    private String holdKey;
    @Column(nullable = false, length = 20)
    private String accountNumber;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Status status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant resolvedAt;

    protected BalanceHold() { }
    public BalanceHold(String holdKey, String accountNumber, BigDecimal amount) {
        this.holdKey = holdKey; this.accountNumber = accountNumber; this.amount = amount;
        this.status = Status.ACTIVE; this.createdAt = Instant.now();
    }
    public String getHoldKey() { return holdKey; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public void settle() { status = Status.SETTLED; resolvedAt = Instant.now(); }
    public void release() { status = Status.RELEASED; resolvedAt = Instant.now(); }
}

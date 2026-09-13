package com.finledger.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.security.SecureRandom;

@Entity
@Table(name = "money_transfers")
public class MoneyTransfer {

    public enum Status { PENDING_HOLD, PENDING_DEBIT, PENDING_CREDIT, COMPENSATION_PENDING, COMPLETED, COMPENSATED, FAILED }
    public enum ReconciliationStatus { PENDING, SETTLED, REVERSED, FAILED }
    private static final SecureRandom REFERENCE_RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String fromAccount;

    @Column(nullable = false, length = 20)
    private String toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /** Customer-visible bank reference, generated once and retained across retries. */
    @Column(nullable = false, unique = true, length = 20)
    private String paymentReference;

    @Column(nullable = false, length = 80)
    private String initiatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ReconciliationStatus reconciliationStatus;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(columnDefinition = "text")
    private String lastFailure;

    @Column(nullable = false)
    private int recoveryAttempts;

    protected MoneyTransfer() {
    }

    public MoneyTransfer(String fromAccount, String toAccount, BigDecimal amount, String idempotencyKey,
                         String initiatedBy) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.paymentReference = nextPaymentReference();
        this.initiatedBy = initiatedBy;
        this.status = Status.PENDING_HOLD;
        this.reconciliationStatus = ReconciliationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getPaymentReference() { return paymentReference; }
    public String getInitiatedBy() { return initiatedBy; }
    public Status getStatus() { return status; }
    public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public String getLastFailure() { return lastFailure; }
    public int getRecoveryAttempts() { return recoveryAttempts; }

    public void markDebitPending() { status = Status.PENDING_DEBIT; lastFailure = null; }
    public void markCreditPending() { status = Status.PENDING_CREDIT; lastFailure = null; }
    public void markCompensationPending(String failure) { status = Status.COMPENSATION_PENDING; recordFailure(failure); }
    public void markCompleted() { status = Status.COMPLETED; reconciliationStatus = ReconciliationStatus.SETTLED; lastFailure = null; }
    public void markCompensated() { status = Status.COMPENSATED; reconciliationStatus = ReconciliationStatus.REVERSED; lastFailure = null; }
    public void markFailed(String failure) { status = Status.FAILED; reconciliationStatus = ReconciliationStatus.FAILED; recordFailure(failure); }
    public void recordFailure(String failure) { lastFailure = failure; recoveryAttempts++; }

    private static String nextPaymentReference() {
        return "UPI" + String.format("%012d", Math.floorMod(REFERENCE_RANDOM.nextLong(), 1_000_000_000_000L));
    }
}

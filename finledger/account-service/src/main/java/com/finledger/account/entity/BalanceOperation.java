package com.finledger.account.entity;

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

@Entity
@Table(name = "balance_operations")
public class BalanceOperation {

    public enum Type { DEBIT, CREDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String operationKey;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 10)
    private Type type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected BalanceOperation() {
    }

    public BalanceOperation(String operationKey, String accountNumber, Type type, BigDecimal amount) {
        this.operationKey = operationKey;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public String getOperationKey() { return operationKey; }
    public String getAccountNumber() { return accountNumber; }
    public Type getType() { return type; }
    public BigDecimal getAmount() { return amount; }
}

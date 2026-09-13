package com.finledger.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 120)
    private String ownerName;

    @Column(nullable = false, length = 80)
    private String ownerUsername;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /** Amount reserved for an authorised but not yet settled payment. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal heldBalance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(String accountNumber, String ownerName, String ownerUsername, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.ownerUsername = ownerUsername;
        this.balance = balance;
        this.heldBalance = BigDecimal.ZERO;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getHeldBalance() { return heldBalance; }

    public BigDecimal getAvailableBalance() { return balance.subtract(heldBalance); }

    public void setHeldBalance(BigDecimal heldBalance) { this.heldBalance = heldBalance; }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

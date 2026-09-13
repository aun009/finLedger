package com.finledger.account.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String accountNumber, BigDecimal balance, BigDecimal requested) {
        super("Insufficient funds in " + accountNumber + ". Available: " + balance + ", requested: " + requested);
    }
}

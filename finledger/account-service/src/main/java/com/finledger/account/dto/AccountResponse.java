package com.finledger.account.dto;

import com.finledger.account.entity.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String accountNumber,
        String ownerName,
        /** Backwards-compatible alias for availableBalance. */
        BigDecimal balance,
        BigDecimal ledgerBalance,
        BigDecimal heldBalance,
        BigDecimal availableBalance,
        Instant createdAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getAvailableBalance(),
                account.getBalance(),
                account.getHeldBalance(),
                account.getAvailableBalance(),
                account.getCreatedAt()
        );
    }
}

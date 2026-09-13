package com.finledger.transaction.dto;

import com.finledger.transaction.entity.MoneyTransfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(UUID id, String fromAccount, String toAccount,
                               BigDecimal amount, String status, String idempotencyKey,
                               String paymentReference, String reconciliationStatus, Instant createdAt) {

    public static TransferResponse from(MoneyTransfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getFromAccount(), transfer.getToAccount(),
                transfer.getAmount(), transfer.getStatus().name(), transfer.getIdempotencyKey(),
                transfer.getPaymentReference(), transfer.getReconciliationStatus().name(), transfer.getCreatedAt());
    }
}

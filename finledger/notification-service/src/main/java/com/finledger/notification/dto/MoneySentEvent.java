package com.finledger.notification.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MoneySentEvent(UUID transferId, String fromAccount, String toAccount,
                             BigDecimal amount, Instant occurredAt) {
}

package com.finledger.transaction.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTransferTests {

    @Test
    void createsPendingPaymentWithCustomerReferenceAndReconcilesItOnCompletion() {
        MoneyTransfer transfer = new MoneyTransfer("ACC00000001", "ACC00000002", new BigDecimal("125.50"),
                "client-request-1", "alice");

        assertEquals(MoneyTransfer.Status.PENDING_HOLD, transfer.getStatus());
        assertEquals(MoneyTransfer.ReconciliationStatus.PENDING, transfer.getReconciliationStatus());
        assertTrue(transfer.getPaymentReference().matches("UPI\\d{12}"));

        transfer.markCompleted();

        assertEquals(MoneyTransfer.ReconciliationStatus.SETTLED, transfer.getReconciliationStatus());
    }

    @Test
    void compensationProducesAReversedReconciliationOutcome() {
        MoneyTransfer transfer = new MoneyTransfer("ACC00000001", "ACC00000002", new BigDecimal("125.50"),
                "client-request-2", "alice");

        transfer.markCompensated();

        assertEquals(MoneyTransfer.Status.COMPENSATED, transfer.getStatus());
        assertEquals(MoneyTransfer.ReconciliationStatus.REVERSED, transfer.getReconciliationStatus());
    }
}

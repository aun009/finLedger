package com.finledger.transaction.service;

import com.finledger.transaction.entity.MoneyTransfer;
import com.finledger.transaction.repository.MoneyTransferRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class TransferRecovery {

    private static final Logger log = LoggerFactory.getLogger(TransferRecovery.class);

    private final MoneyTransferRepository transferRepository;
    private final TransferService transferService;

    public TransferRecovery(MoneyTransferRepository transferRepository, TransferService transferService) {
        this.transferRepository = transferRepository;
        this.transferService = transferService;
    }

    @Scheduled(fixedDelayString = "${transfer.recovery.delay-ms:10000}")
    public void recoverPendingTransfers() {
        transferRepository.findTop100ByStatusInOrderByCreatedAtAsc(List.of(
                        MoneyTransfer.Status.PENDING_HOLD,
                        MoneyTransfer.Status.PENDING_DEBIT,
                        MoneyTransfer.Status.PENDING_CREDIT,
                        MoneyTransfer.Status.COMPENSATION_PENDING))
                .forEach(transfer -> {
                    try {
                        transferService.recover(transfer.getId());
                    } catch (RuntimeException exception) {
                        log.warn("UPI reconciliation could not resolve payment {} (transfer {}). It will be retried",
                                transfer.getPaymentReference(), transfer.getId(), exception);
                    }
                });
    }
}

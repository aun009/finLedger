package com.finledger.transaction.service;

import com.finledger.transaction.dto.TransferRequest;
import com.finledger.transaction.dto.TransferResponse;
import com.finledger.transaction.dto.AccountAmountRequest;
import com.finledger.transaction.entity.MoneyTransfer;
import com.finledger.transaction.repository.MoneyTransferRepository;
import com.finledger.transaction.exception.TransferNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;
import java.util.List;

@Service
public class TransferService {

    private final MoneyTransferRepository transferRepository;
    private final RestClient accountRestClient;
    private final TransferLifecycleService lifecycleService;

    public TransferService(MoneyTransferRepository transferRepository, RestClient accountRestClient,
                           TransferLifecycleService lifecycleService) {
        this.transferRepository = transferRepository;
        this.accountRestClient = accountRestClient;
        this.lifecycleService = lifecycleService;
    }

    public TransferResponse transfer(TransferRequest request, String idempotencyKey, String initiatedBy) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("Idempotency-Key is required and must be at most 100 characters");
        }
        MoneyTransfer transfer = transferRepository.findByIdempotencyKey(idempotencyKey)
                .map(existingTransfer -> {
                    verifyInitiator(existingTransfer, initiatedBy);
                    return existingTransfer;
                })
                .orElseGet(() -> createTransfer(request, idempotencyKey, initiatedBy));
        return TransferResponse.from(process(transfer));
    }

    public TransferResponse findById(UUID transferId, String initiatedBy) {
        return transferRepository.findById(transferId)
                .map(transfer -> {
                    verifyInitiator(transfer, initiatedBy);
                    return TransferResponse.from(transfer);
                })
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    public List<TransferResponse> findRecent(String initiatedBy) {
        return transferRepository.findTop50ByInitiatedByOrderByCreatedAtDesc(initiatedBy).stream()
                .map(TransferResponse::from).toList();
    }

    public void recover(UUID transferId) {
        transferRepository.findById(transferId).ifPresent(this::process);
    }

    private MoneyTransfer createTransfer(TransferRequest request, String idempotencyKey, String initiatedBy) {
        if (request.fromAccount().equals(request.toAccount())) {
            throw new IllegalArgumentException("source and destination accounts must be different");
        }
        try {
            return lifecycleService.create(new MoneyTransfer(request.fromAccount(), request.toAccount(),
                    request.amount(), idempotencyKey, initiatedBy));
        } catch (DataIntegrityViolationException exception) {
            MoneyTransfer existingTransfer = transferRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
            verifyInitiator(existingTransfer, initiatedBy);
            return existingTransfer;
        }
    }

    private MoneyTransfer process(MoneyTransfer transfer) {
        try {
            return switch (transfer.getStatus()) {
                case PENDING_HOLD -> holdAndContinue(transfer);
                case PENDING_DEBIT -> debitAndContinue(transfer);
                case PENDING_CREDIT -> creditAndComplete(transfer);
                case COMPENSATION_PENDING -> compensate(transfer);
                case COMPLETED, COMPENSATED, FAILED -> transfer;
            };
        } catch (RestClientResponseException exception) {
            handlePermanentFailure(transfer, exception);
            throw exception;
        } catch (RuntimeException exception) {
            return lifecycleService.recordTransientFailure(transfer.getId(), failureMessage(exception));
        }
    }

    private MoneyTransfer holdAndContinue(MoneyTransfer transfer) {
        accountRestClient.post()
                .uri("/accounts/{accountNumber}/holds", transfer.getFromAccount())
                .header("X-Account-Owner", transfer.getInitiatedBy())
                .header("X-Hold-Key", holdKey(transfer))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccountAmountRequest(transfer.getAmount()))
                .retrieve().toBodilessEntity();
        return debitAndContinue(lifecycleService.markDebitPending(transfer.getId()));
    }

    private MoneyTransfer debitAndContinue(MoneyTransfer transfer) {
        accountRestClient.post()
                .uri("/accounts/{accountNumber}/holds/{holdKey}/settle", transfer.getFromAccount(), holdKey(transfer))
                .retrieve()
                .toBodilessEntity();
        return creditAndComplete(lifecycleService.markCreditPending(transfer.getId()));
    }

    private MoneyTransfer creditAndComplete(MoneyTransfer transfer) {
        accountRestClient.post()
                .uri("/accounts/{accountNumber}/credit", transfer.getToAccount())
                .header("X-Balance-Operation", operationKey(transfer, "credit"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccountAmountRequest(transfer.getAmount()))
                .retrieve()
                .toBodilessEntity();
        return lifecycleService.complete(transfer.getId());
    }

    private MoneyTransfer compensate(MoneyTransfer transfer) {
        accountRestClient.post()
                .uri("/accounts/{accountNumber}/credit", transfer.getFromAccount())
                .header("X-Balance-Operation", operationKey(transfer, "refund"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccountAmountRequest(transfer.getAmount()))
                .retrieve()
                .toBodilessEntity();
        return lifecycleService.markCompensated(transfer.getId());
    }

    private void handlePermanentFailure(MoneyTransfer transfer, RestClientResponseException exception) {
        String failure = failureMessage(exception);
        if (transfer.getStatus() == MoneyTransfer.Status.PENDING_HOLD || transfer.getStatus() == MoneyTransfer.Status.PENDING_DEBIT) {
            lifecycleService.markFailed(transfer.getId(), failure);
        } else if (transfer.getStatus() == MoneyTransfer.Status.PENDING_CREDIT) {
            lifecycleService.markCompensationPending(transfer.getId(), failure);
            compensate(transfer);
        } else {
            lifecycleService.recordTransientFailure(transfer.getId(), failure);
        }
    }

    private String operationKey(MoneyTransfer transfer, String operation) {
        return transfer.getId() + ":" + operation;
    }

    private String holdKey(MoneyTransfer transfer) { return transfer.getId() + ":hold"; }

    private String failureMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message.substring(0, Math.min(message.length(), 1000));
    }

    private void verifyInitiator(MoneyTransfer transfer, String initiatedBy) {
        if (!transfer.getInitiatedBy().equals(initiatedBy)) {
            throw new org.springframework.security.access.AccessDeniedException("transfer does not belong to the authenticated user");
        }
    }
}

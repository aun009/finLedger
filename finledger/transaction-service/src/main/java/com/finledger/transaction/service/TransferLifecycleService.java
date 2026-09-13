package com.finledger.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finledger.transaction.dto.MoneySentEvent;
import com.finledger.transaction.entity.MoneyTransfer;
import com.finledger.transaction.entity.OutboxEvent;
import com.finledger.transaction.repository.MoneyTransferRepository;
import com.finledger.transaction.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransferLifecycleService {

    private final MoneyTransferRepository transferRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public TransferLifecycleService(MoneyTransferRepository transferRepository,
                                    OutboxEventRepository outboxEventRepository,
                                    ObjectMapper objectMapper) {
        this.transferRepository = transferRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MoneyTransfer create(MoneyTransfer transfer) {
        return transferRepository.saveAndFlush(transfer);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MoneyTransfer markDebitPending(UUID transferId) {
        MoneyTransfer transfer = requiredTransfer(transferId);
        transfer.markDebitPending();
        return transfer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MoneyTransfer markCreditPending(UUID transferId) {
        MoneyTransfer transfer = requiredTransfer(transferId);
        transfer.markCreditPending();
        return transfer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MoneyTransfer recordTransientFailure(UUID transferId, String failure) {
        MoneyTransfer transfer = requiredTransfer(transferId);
        transfer.recordFailure(failure);
        return transfer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MoneyTransfer markFailed(UUID transferId, String failure) {
        MoneyTransfer transfer = requiredTransfer(transferId);
        transfer.markFailed(failure);
        return transfer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MoneyTransfer markCompensationPending(UUID transferId, String failure) {
        MoneyTransfer transfer = requiredTransfer(transferId);
        transfer.markCompensationPending(failure);
        return transfer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MoneyTransfer markCompensated(UUID transferId) {
        MoneyTransfer transfer = requiredTransfer(transferId);
        transfer.markCompensated();
        return transfer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MoneyTransfer complete(UUID transferId) {
        MoneyTransfer transfer = requiredTransfer(transferId);
        transfer.markCompleted();
        if (!outboxEventRepository.existsByTopicAndEventKey("money.sent", transferId.toString())) {
            try {
                MoneySentEvent event = new MoneySentEvent(transfer.getId(), transfer.getFromAccount(),
                        transfer.getToAccount(), transfer.getAmount(), transfer.getCreatedAt());
                outboxEventRepository.save(new OutboxEvent(
                        "money.sent", transferId.toString(), objectMapper.writeValueAsString(event)));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Could not serialize money.sent event for transfer " + transferId, exception);
            }
        }
        return transfer;
    }

    private MoneyTransfer requiredTransfer(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("transfer disappeared during recovery: " + transferId));
    }
}

package com.finledger.transaction.repository;

import com.finledger.transaction.entity.MoneyTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MoneyTransferRepository extends JpaRepository<MoneyTransfer, UUID> {

    Optional<MoneyTransfer> findByIdempotencyKey(String idempotencyKey);

    List<MoneyTransfer> findTop100ByStatusInOrderByCreatedAtAsc(Collection<MoneyTransfer.Status> statuses);

    List<MoneyTransfer> findTop50ByInitiatedByOrderByCreatedAtDesc(String initiatedBy);
}

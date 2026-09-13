package com.finledger.account.repository;

import com.finledger.account.entity.BalanceOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BalanceOperationRepository extends JpaRepository<BalanceOperation, UUID> {
    Optional<BalanceOperation> findByOperationKey(String operationKey);
}

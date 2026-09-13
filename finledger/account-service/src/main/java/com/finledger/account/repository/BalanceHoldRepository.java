package com.finledger.account.repository;

import com.finledger.account.entity.BalanceHold;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BalanceHoldRepository extends JpaRepository<BalanceHold, UUID> {
    Optional<BalanceHold> findByHoldKey(String holdKey);
}

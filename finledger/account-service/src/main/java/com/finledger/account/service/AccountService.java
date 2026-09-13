package com.finledger.account.service;

import com.finledger.account.dto.AccountResponse;
import com.finledger.account.dto.CreateAccountRequest;
import com.finledger.account.dto.HoldResponse;
import com.finledger.account.entity.Account;
import com.finledger.account.entity.BalanceOperation;
import com.finledger.account.entity.BalanceHold;
import com.finledger.account.exception.AccountNotFoundException;
import com.finledger.account.exception.InsufficientFundsException;
import com.finledger.account.repository.AccountRepository;
import com.finledger.account.repository.BalanceOperationRepository;
import com.finledger.account.repository.BalanceHoldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final BalanceOperationRepository balanceOperationRepository;
    private final BalanceHoldRepository balanceHoldRepository;

    public AccountService(AccountRepository accountRepository, BalanceOperationRepository balanceOperationRepository,
                          BalanceHoldRepository balanceHoldRepository) {
        this.accountRepository = accountRepository;
        this.balanceOperationRepository = balanceOperationRepository;
        this.balanceHoldRepository = balanceHoldRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request, String ownerUsername) {
        BigDecimal initialBalance = request.initialBalance() == null
                ? BigDecimal.ZERO
                : request.initialBalance();

        Account account = new Account(generateAccountNumber(), request.ownerName().trim(), ownerUsername, initialBalance);
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse findByNumber(String accountNumber, String ownerUsername) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        verifyOwnership(account, ownerUsername);
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll(String ownerUsername) {
        return accountRepository.findByOwnerUsernameOrderByCreatedAtDesc(ownerUsername).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional
    public AccountResponse debit(String accountNumber, BigDecimal amount, String ownerUsername, String operationKey) {
        validateAmount(amount);
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        verifyOwnership(account, ownerUsername);
        AccountResponse priorResult = findPriorOperation(operationKey, accountNumber, BalanceOperation.Type.DEBIT, amount);
        if (priorResult != null) {
            return priorResult;
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber, account.getBalance(), amount);
        }
        account.setBalance(account.getBalance().subtract(amount));
        AccountResponse response = AccountResponse.from(accountRepository.save(account));
        balanceOperationRepository.save(new BalanceOperation(operationKey, accountNumber, BalanceOperation.Type.DEBIT, amount));
        return response;
    }

    @Transactional
    public AccountResponse credit(String accountNumber, BigDecimal amount, String operationKey) {
        validateAmount(amount);
        AccountResponse priorResult = findPriorOperation(operationKey, accountNumber, BalanceOperation.Type.CREDIT, amount);
        if (priorResult != null) {
            return priorResult;
        }
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        account.setBalance(account.getBalance().add(amount));
        AccountResponse response = AccountResponse.from(accountRepository.save(account));
        balanceOperationRepository.save(new BalanceOperation(operationKey, accountNumber, BalanceOperation.Type.CREDIT, amount));
        return response;
    }

    /** Reserve money before asynchronous payment settlement. The reservation is idempotent by hold key. */
    @Transactional
    public HoldResponse placeHold(String accountNumber, BigDecimal amount, String ownerUsername, String holdKey) {
        validateAmount(amount);
        validateOperationKey(holdKey, "X-Hold-Key");
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        verifyOwnership(account, ownerUsername);
        BalanceHold existing = balanceHoldRepository.findByHoldKey(holdKey).orElse(null);
        if (existing != null) {
            validateMatchingHold(existing, accountNumber, amount);
            return HoldResponse.from(existing, account);
        }
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber, account.getAvailableBalance(), amount);
        }
        BalanceHold hold = balanceHoldRepository.save(new BalanceHold(holdKey, accountNumber, amount));
        account.setHeldBalance(account.getHeldBalance().add(amount));
        return HoldResponse.from(hold, accountRepository.save(account));
    }

    /** Converts an active hold into a posted debit exactly once. */
    @Transactional
    public HoldResponse settleHold(String accountNumber, String holdKey) {
        validateOperationKey(holdKey, "X-Hold-Key");
        BalanceHold hold = requiredHold(holdKey, accountNumber);
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        if (hold.getStatus() == BalanceHold.Status.SETTLED) return HoldResponse.from(hold, account);
        if (hold.getStatus() != BalanceHold.Status.ACTIVE) throw new IllegalStateException("released hold cannot be settled");
        account.setHeldBalance(account.getHeldBalance().subtract(hold.getAmount()));
        account.setBalance(account.getBalance().subtract(hold.getAmount()));
        hold.settle();
        balanceOperationRepository.save(new BalanceOperation(holdKey + ":settlement", accountNumber,
                BalanceOperation.Type.DEBIT, hold.getAmount()));
        return HoldResponse.from(balanceHoldRepository.save(hold), accountRepository.save(account));
    }

    /** Releases an active hold when a payment is declined, cancelled, or expires. */
    @Transactional
    public HoldResponse releaseHold(String accountNumber, String holdKey) {
        validateOperationKey(holdKey, "X-Hold-Key");
        BalanceHold hold = requiredHold(holdKey, accountNumber);
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        if (hold.getStatus() == BalanceHold.Status.RELEASED) return HoldResponse.from(hold, account);
        if (hold.getStatus() != BalanceHold.Status.ACTIVE) throw new IllegalStateException("settled hold cannot be released");
        account.setHeldBalance(account.getHeldBalance().subtract(hold.getAmount()));
        hold.release();
        return HoldResponse.from(balanceHoldRepository.save(hold), accountRepository.save(account));
    }

    private String generateAccountNumber() {
        return "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }

    private AccountResponse findPriorOperation(String operationKey, String accountNumber,
                                               BalanceOperation.Type type, BigDecimal amount) {
        validateOperationKey(operationKey, "X-Balance-Operation");
        return balanceOperationRepository.findByOperationKey(operationKey)
                .map(operation -> {
                    if (!operation.getAccountNumber().equals(accountNumber)
                            || operation.getType() != type
                            || operation.getAmount().compareTo(amount) != 0) {
                        throw new IllegalArgumentException("X-Balance-Operation cannot be reused for a different balance change");
                    }
                    return accountRepository.findByAccountNumber(accountNumber)
                            .map(AccountResponse::from)
                            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
                })
                .orElse(null);
    }

    private BalanceHold requiredHold(String holdKey, String accountNumber) {
        BalanceHold hold = balanceHoldRepository.findByHoldKey(holdKey)
                .orElseThrow(() -> new IllegalArgumentException("hold does not exist"));
        if (!hold.getAccountNumber().equals(accountNumber)) throw new IllegalArgumentException("hold belongs to another account");
        return hold;
    }

    private void validateMatchingHold(BalanceHold hold, String accountNumber, BigDecimal amount) {
        if (!hold.getAccountNumber().equals(accountNumber) || hold.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("X-Hold-Key cannot be reused for a different reservation");
        }
    }

    private void validateOperationKey(String operationKey, String headerName) {
        if (operationKey == null || operationKey.isBlank() || operationKey.length() > 100) {
            throw new IllegalArgumentException(headerName + " is required and must be at most 100 characters");
        }
    }

    private void verifyOwnership(Account account, String ownerUsername) {
        if (!account.getOwnerUsername().equals(ownerUsername)) {
            throw new org.springframework.security.access.AccessDeniedException("account does not belong to the authenticated user");
        }
    }
}

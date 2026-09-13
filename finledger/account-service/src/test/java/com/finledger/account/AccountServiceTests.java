package com.finledger.account;

import com.finledger.account.dto.AccountResponse;
import com.finledger.account.dto.CreateAccountRequest;
import com.finledger.account.dto.HoldResponse;
import com.finledger.account.exception.InsufficientFundsException;
import com.finledger.account.service.AccountService;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(AccountService.class)
@ActiveProfiles("test")
class AccountServiceTests {

    @Autowired
    private AccountService accountService;

    @Test
    void createsAndReadsAccount() {
        AccountResponse created = accountService.create(
                new CreateAccountRequest(" Alice ", new BigDecimal("100.00")), "alice");

        AccountResponse found = accountService.findByNumber(created.accountNumber(), "alice");

        assertEquals("Alice", found.ownerName());
        assertEquals(new BigDecimal("100.00"), found.ledgerBalance());
        assertEquals(new BigDecimal("100.00"), found.availableBalance());
    }

    @Test
    void debitAndCreditUpdateBalance() {
        AccountResponse created = accountService.create(
                new CreateAccountRequest("Alice", new BigDecimal("100.00")), "alice");

        accountService.debit(created.accountNumber(), new BigDecimal("25.00"), "alice", "transfer-1-debit");
        AccountResponse credited = accountService.credit(created.accountNumber(), new BigDecimal("10.00"), "transfer-1-credit");

        assertEquals(new BigDecimal("85.00"), credited.ledgerBalance());
    }

    @Test
    void rejectsDebitAboveBalance() {
        AccountResponse created = accountService.create(
                new CreateAccountRequest("Alice", new BigDecimal("10.00")), "alice");

        assertThrows(InsufficientFundsException.class,
                () -> accountService.debit(created.accountNumber(), new BigDecimal("10.01"), "alice", "transfer-2-debit"));
    }

    @Test
    void preventsAnotherUserFromReadingOrDebitingAnAccount() {
        AccountResponse created = accountService.create(
                new CreateAccountRequest("Alice", new BigDecimal("100.00")), "alice");

        assertThrows(AccessDeniedException.class,
                () -> accountService.findByNumber(created.accountNumber(), "bob"));
        assertThrows(AccessDeniedException.class,
                () -> accountService.debit(created.accountNumber(), new BigDecimal("10.00"), "bob", "transfer-3-debit"));
    }

    @Test
    void repeatsTheSameBalanceOperationOnlyOnce() {
        AccountResponse created = accountService.create(
                new CreateAccountRequest("Alice", new BigDecimal("100.00")), "alice");

        accountService.debit(created.accountNumber(), new BigDecimal("25.00"), "alice", "transfer-4-debit");
        AccountResponse repeated = accountService.debit(
                created.accountNumber(), new BigDecimal("25.00"), "alice", "transfer-4-debit");

        assertEquals(new BigDecimal("75.00"), repeated.ledgerBalance());
    }

    @Test
    void holdReducesAvailableBalanceUntilSettlementPostsTheDebit() {
        AccountResponse created = accountService.create(
                new CreateAccountRequest("Alice", new BigDecimal("100.00")), "alice");

        HoldResponse hold = accountService.placeHold(created.accountNumber(), new BigDecimal("25.00"), "alice", "payment-1");
        assertEquals(new BigDecimal("100.00"), hold.account().ledgerBalance());
        assertEquals(new BigDecimal("25.00"), hold.account().heldBalance());
        assertEquals(new BigDecimal("75.00"), hold.account().availableBalance());

        HoldResponse settled = accountService.settleHold(created.accountNumber(), "payment-1");
        assertEquals("SETTLED", settled.status());
        assertEquals(new BigDecimal("75.00"), settled.account().ledgerBalance());
        assertEquals(new BigDecimal("0.00"), settled.account().heldBalance());
    }

    @Test
    void releasedHoldMakesFundsAvailableAgain() {
        AccountResponse created = accountService.create(
                new CreateAccountRequest("Alice", new BigDecimal("100.00")), "alice");
        accountService.placeHold(created.accountNumber(), new BigDecimal("25.00"), "alice", "payment-2");

        HoldResponse released = accountService.releaseHold(created.accountNumber(), "payment-2");
        assertEquals("RELEASED", released.status());
        assertEquals(new BigDecimal("100.00"), released.account().ledgerBalance());
        assertEquals(new BigDecimal("100.00"), released.account().availableBalance());
    }
}

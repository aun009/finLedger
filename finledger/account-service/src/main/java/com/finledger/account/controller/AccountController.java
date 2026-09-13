package com.finledger.account.controller;

import com.finledger.account.dto.AccountResponse;
import com.finledger.account.dto.AmountRequest;
import com.finledger.account.dto.CreateAccountRequest;
import com.finledger.account.dto.HoldResponse;
import com.finledger.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request,
                                                  @AuthenticationPrincipal String username) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request, username));
    }

    @GetMapping
    public java.util.List<AccountResponse> findAll(@AuthenticationPrincipal String username) {
        return accountService.findAll(username);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse findByNumber(@PathVariable String accountNumber,
                                        @AuthenticationPrincipal String username) {
        return accountService.findByNumber(accountNumber, username);
    }

    @PostMapping("/{accountNumber}/debit")
    public AccountResponse debit(@PathVariable String accountNumber,
                                 @Valid @RequestBody AmountRequest request,
                                 @RequestHeader("X-Account-Owner") String ownerUsername,
                                 @RequestHeader("X-Balance-Operation") String operationKey) {
        return accountService.debit(accountNumber, request.amount(), ownerUsername, operationKey);
    }

    @PostMapping("/{accountNumber}/credit")
    public AccountResponse credit(@PathVariable String accountNumber,
                                  @Valid @RequestBody AmountRequest request,
                                  @RequestHeader("X-Balance-Operation") String operationKey) {
        return accountService.credit(accountNumber, request.amount(), operationKey);
    }

    @PostMapping("/{accountNumber}/holds")
    public ResponseEntity<HoldResponse> placeHold(@PathVariable String accountNumber,
                                                  @Valid @RequestBody AmountRequest request,
                                                  @RequestHeader("X-Account-Owner") String ownerUsername,
                                                  @RequestHeader("X-Hold-Key") String holdKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.placeHold(accountNumber, request.amount(), ownerUsername, holdKey));
    }

    @PostMapping("/{accountNumber}/holds/{holdKey}/settle")
    public HoldResponse settleHold(@PathVariable String accountNumber, @PathVariable String holdKey) {
        return accountService.settleHold(accountNumber, holdKey);
    }

    @PostMapping("/{accountNumber}/holds/{holdKey}/release")
    public HoldResponse releaseHold(@PathVariable String accountNumber, @PathVariable String holdKey) {
        return accountService.releaseHold(accountNumber, holdKey);
    }
}

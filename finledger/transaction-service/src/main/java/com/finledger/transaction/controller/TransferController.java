package com.finledger.transaction.controller;

import com.finledger.transaction.dto.TransferRequest;
import com.finledger.transaction.dto.TransferResponse;
import com.finledger.transaction.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@Validated
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("Idempotency-Key") @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal String username) {
        TransferResponse response = transferService.transfer(request, idempotencyKey, username);
        HttpStatus status = switch (response.status()) {
            case "COMPLETED" -> HttpStatus.CREATED;
            case "FAILED", "COMPENSATED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.ACCEPTED;
        };
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{transferId}")
    public TransferResponse findById(@PathVariable UUID transferId, @AuthenticationPrincipal String username) {
        return transferService.findById(transferId, username);
    }

    @GetMapping
    public java.util.List<TransferResponse> findRecent(@AuthenticationPrincipal String username) {
        return transferService.findRecent(username);
    }
}

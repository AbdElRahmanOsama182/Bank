package com.example.transactionservice.controller;

import com.example.transactionservice.dto.TransferExecutionRequest;
import com.example.transactionservice.dto.TransferInitiationRequest;
import com.example.transactionservice.dto.TransactionResponse;
import com.example.transactionservice.service.TransactionService;
import com.example.userservice.dto.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @PostMapping("/transfer/initiation")
    public ResponseEntity<?> initiateTransfer(@Valid @RequestBody TransferInitiationRequest request) {
        try {
            TransactionResponse response = transactionService.initiateTransfer(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Transfer initiation failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/transfer/execution")
    public ResponseEntity<?> executeTransfer(@Valid @RequestBody TransferExecutionRequest request) {
        try {
            TransactionResponse response = transactionService.executeTransfer(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Transfer execution failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<?> getAccountTransactions(@PathVariable UUID accountId) {
        try {
            List<TransactionResponse> response = transactionService.getAccountTransactions(accountId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Transaction retrieval failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .error("Not Found")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
} 
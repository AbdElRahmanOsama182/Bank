package com.example.accountservice.controller;

import com.example.accountservice.dto.AccountCreationRequest;
import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.TransferRequest;
import com.example.accountservice.dto.TransferResponse;
import com.example.accountservice.service.AccountService;
import com.example.userservice.dto.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AccountController {
    
    private final AccountService accountService;
    
    @PostMapping
    public ResponseEntity<?> createAccount(@Valid @RequestBody AccountCreationRequest request) {
        try {
            log.info("Creating account for user from Postman: {}", request.getUserId());
            accountService.sendLog(request, "Request");
            AccountResponse response = accountService.createAccount(request);
            accountService.sendLog(response,"Response");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Account creation failed: {}", e.getMessage());

            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message(e.getMessage())
                    .build();
            accountService.sendLog(response, "Response");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccount(@PathVariable UUID accountId) {
        try {
            AccountResponse response = accountService.getAccount(accountId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Account retrieval failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .error("Not Found")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    @GetMapping("/users/{userId}/accounts")
    public ResponseEntity<?> getUserAccounts(@PathVariable UUID userId) {
        try {
            List<AccountResponse> response = accountService.getUserAccounts(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("User accounts retrieval failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .error("Not Found")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    @PutMapping("/transfer")
    public ResponseEntity<?> updateAccountBalance(@Valid @RequestBody TransferRequest request) {
        try {
            TransferResponse response = accountService.updateAccountBalance(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Account balance update failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
} 
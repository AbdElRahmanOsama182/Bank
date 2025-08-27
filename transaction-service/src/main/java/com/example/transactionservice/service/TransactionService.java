package com.example.transactionservice.service;

import com.example.transactionservice.dto.TransferExecutionRequest;
import com.example.transactionservice.dto.TransferInitiationRequest;
import com.example.transactionservice.dto.TransactionResponse;
import com.example.transactionservice.enums.TransactionStatus;
import com.example.transactionservice.model.Transaction;
import com.example.transactionservice.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final AccountTransactionService accountTransactionService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${account.service.url}")
    private String accountServiceUrl;
    
    public TransactionResponse initiateTransfer(TransferInitiationRequest request) {
        log.info("Initiating transfer: {} -> {}, amount: {}", 
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        
        // Validate that both accounts exist
        accountTransactionService.validateAccountsExist(request.getFromAccountId(), request.getToAccountId());
        
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(request.getFromAccountId());
        transaction.setToAccountId(request.getToAccountId());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setStatus(TransactionStatus.INITIATED);
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        log.info("Transfer initiated successfully. Transaction ID: {}", savedTransaction.getTransactionId());
        
        return TransactionResponse.builder()
                .transactionId(savedTransaction.getTransactionId())
                .status(savedTransaction.getStatus())
                .timestamp(savedTransaction.getTimestamp())
                .build();
    }
    
    public TransactionResponse executeTransfer(TransferExecutionRequest request) {
        log.info("Executing transfer: {}", request.getTransactionId());
        
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new RuntimeException("Transaction is not in INITIATED status");
        }
        
        try {
            // Validate sufficient funds before executing transfer
            accountTransactionService.validateSufficientFunds(transaction.getFromAccountId(), transaction.getAmount());
            
            // Call Account Service to update balances
            accountTransactionService.updateAccountsBalance(transaction.getFromAccountId(), transaction.getToAccountId(), transaction.getAmount());
            
            // Update transaction status
            transaction.setStatus(TransactionStatus.SUCCESS);
            
            Transaction savedTransaction = transactionRepository.save(transaction);
            
            log.info("Transfer executed successfully. Transaction ID: {}", savedTransaction.getTransactionId());
            
            return TransactionResponse.builder()
                    .transactionId(savedTransaction.getTransactionId())
                    .status(savedTransaction.getStatus())
                    .timestamp(savedTransaction.getTimestamp())
                    .build();
                    
        } catch (Exception e) {
            log.error("Transfer execution failed: {}", e.getMessage());
            
            // Update transaction status to failed
            transaction.setStatus(TransactionStatus.FAILED);
            Transaction savedTransaction = transactionRepository.save(transaction);
            log.info("Transfer failed. Transaction ID: {}", savedTransaction.getTransactionId());
            
            throw new RuntimeException("Transfer execution failed: " + e.getMessage());
        }
    }
    
    public List<TransactionResponse> getAccountTransactions(UUID accountId) {
        log.info("Fetching transactions for account: {}", accountId);

        if (!accountTransactionService.validateAccountExists(accountId)) {
            throw new RuntimeException("Account not found: " + accountId);
        }
        
        List<Transaction> transactions = transactionRepository
                .findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);
        
        if (transactions.isEmpty()) {
            throw new RuntimeException("No transactions found for account: " + accountId);
        }
        
        return transactions.stream()
                .map(transaction -> TransactionResponse.builder()
                        .transactionId(transaction.getTransactionId())
                        .fromAccountId(transaction.getFromAccountId())
                        .toAccountId(transaction.getToAccountId())
                        .amount(transaction.getAmount())
                        .description(transaction.getDescription())
                        .timestamp(transaction.getTimestamp())
                        .status(transaction.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
    public void sendLog(String message, String messageType) {
        Map<String, Object> payload = Map.of(
                "message", message,
                "messageType", messageType,
                "dateTime", Instant.now().toString());

        try {
            String json = new ObjectMapper().writeValueAsString(payload);
            kafkaTemplate.send("logging-topic", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
    public void  sendLog(Object json,String messageType) {
        try {
            String flattenJSON = objectMapper.writeValueAsString(json);
            log.info("Sending log to Kafka: {}", flattenJSON);
            sendLog(flattenJSON, messageType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }
} 
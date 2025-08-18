package com.example.transactionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountTransactionService {
    
    private final WebClient webClient;
    
    @Value("${account.service.url}")
    private String accountServiceUrl;
    
    /**
     * Validates that both accounts exist
     * @param fromAccountId The source account ID
     * @param toAccountId The destination account ID
     * @throws RuntimeException if either account doesn't exist
     */
    public void validateAccountsExist(UUID fromAccountId, UUID toAccountId) {
        log.info("Validating accounts exist: from={}, to={}", fromAccountId, toAccountId);
        
        // Validate from account exists
        if (!validateAccountExists(fromAccountId)) {
            throw new RuntimeException("From account not found: " + fromAccountId);
        }
        
        // Validate to account exists
        if (!validateAccountExists(toAccountId)) {
            throw new RuntimeException("To account not found: " + toAccountId);
        }
        
        log.info("Account validation successful for transfer: {} -> {}", fromAccountId, toAccountId);
    }
    
    /**
     * Validates that the from account has sufficient funds
     * @param fromAccountId The source account ID
     * @param amount The transfer amount
     * @throws RuntimeException if insufficient funds
     */
    public void validateSufficientFunds(UUID fromAccountId, BigDecimal amount) {
        log.info("Validating sufficient funds: account={}, amount={}", fromAccountId, amount);
        
        try {
            // Get account details from Account Service
            AccountResponse accountDetails = webClient.get()
                    .uri(accountServiceUrl + "/accounts/" + fromAccountId)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();
            if (accountDetails == null) {
                throw new RuntimeException("Account not found: " + fromAccountId);
            }

            BigDecimal currentBalance = accountDetails.getBalance();
            
            if (currentBalance.compareTo(amount) < 0) {
                String errorMessage = String.format(
                    "Insufficient funds. Account balance: %s, Transfer amount: %s", 
                    currentBalance, amount
                );
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
            
            log.info("Sufficient funds validated. Balance: {}, Transfer amount: {}", currentBalance, amount);
            
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Account not found: " + fromAccountId);
        } catch (WebClientResponseException e) {
            log.error("Error calling Account Service: {}", e.getMessage());
            throw new RuntimeException("Error validating account: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during fund validation: {}", e.getMessage());
            throw new RuntimeException("Error validating sufficient funds: " + e.getMessage());
        }
    }
    
    public boolean validateAccountExists(UUID accountId) {
        try {
            webClient.get()
                    .uri(accountServiceUrl + "/accounts/" + accountId)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();
            
            return true;
        } catch (WebClientResponseException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.error("Error validating account: {}", e.getMessage());
            throw new RuntimeException("Error validating account: " + e.getMessage());
        }
    }

    public void updateAccountsBalance(UUID fromAccountId, UUID toAccountId, BigDecimal amount) {
        log.info("Updating accounts balance: from={}, to={}, amount={}", fromAccountId, toAccountId, amount);

        try {
            // Call Account Service to update balances
            TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, amount);

            // Call Account Service
            var response = webClient.put()
                    .uri(accountServiceUrl + "/accounts/transfer")
                    .bodyValue(transferRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("error") == null) {
                log.info("Accounts balance updated successfully: from={}, to={}, amount={}", fromAccountId, toAccountId, amount);
            } else {
                log.error("Error updating accounts balance: {}", response.get("error"));
                throw new RuntimeException("Error updating accounts balance: " + response.get("error"));
            }
        } catch (Exception e) {
            log.error("Error updating accounts balance: {}", e.getMessage());
            throw new RuntimeException("Error updating accounts balance: " + e.getMessage());
        }
    }
} 
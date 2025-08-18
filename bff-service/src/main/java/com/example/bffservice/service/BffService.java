package com.example.bffservice.service;

import com.example.bffservice.dto.DashboardResponse;
import com.example.transactionservice.dto.TransactionResponse;
import com.example.bffservice.model.UserAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.UUID;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class BffService {
    
    private final WebClient webClient;
    
    @Value("${user.service.url}")
    private String userServiceUrl;
    
    @Value("${account.service.url}")
    private String accountServiceUrl;
    
    @Value("${transaction.service.url}")
    private String transactionServiceUrl;
    
    public DashboardResponse getDashboard(UUID userId) {
        log.info("Fetching dashboard for user: {}", userId);
        try {
            DashboardResponse response = webClient.get()
                .uri(userServiceUrl + "/users/" + userId + "/profile")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DashboardResponse>() {})
                .block();
            List<UserAccount> accounts = getUserAccountsWithTransactions(userId);
            if (!accounts.isEmpty()) {
                response.setAccounts(accounts);
            }
            return response;  
        } catch (Exception e) {
            log.warn("Failed to get dashboard for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get dashboard for user: " + userId);
        }         
    }



    public List<UserAccount> getUserAccountsWithTransactions(UUID userId) {
        try {
            // Get user accounts first
            List<UserAccount> accounts = webClient.get()
                    .uri(accountServiceUrl + "/accounts/users/" + userId + "/accounts")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UserAccount>>() {})
                    .block();
    
            if (accounts == null || accounts.isEmpty()) {
                log.info("No accounts found for user {}", userId);
                return List.of();
            }
    
            // Fetch transactions for each account asynchronously
            return Flux.fromIterable(accounts)
                    .flatMap(account -> webClient.get()
                            .uri(transactionServiceUrl + "/transactions/accounts/" + account.getAccountId())
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<List<TransactionResponse>>() {})
                            // If 404 -> set transactions = null
                            .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                                log.warn("Transactions not found for account {}", account.getAccountId());
                                return Mono.just(List.<TransactionResponse>of()); // return dummy to continue flow
                            })
                            // For other errors -> still continue, set null
                            .onErrorResume(e -> {
                                log.error("Error fetching transactions for account {}: {}", account.getAccountId(), e.getMessage());
                                return Mono.just(List.<TransactionResponse>of());
                            })
                            .map(transactions -> {
                                // If not 404, set transactions normally
                                if (account.getTransactions() == null && transactions.size() > 0) {
                                    account.setTransactions(transactions);
                                }
                                return account;
                            })
                    )
                    .collectList()
                    .block(); // blocking since method returns List
        } catch (Exception e) {
            log.warn("Failed to get accounts with transactions for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }
} 
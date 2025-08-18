package com.example.accountservice.service;

import com.example.accountservice.dto.AccountCreationRequest;
import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.TransferRequest;
import com.example.accountservice.dto.TransferResponse;
import com.example.accountservice.model.Account;
import com.example.accountservice.enums.AccountStatus;
import com.example.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final UserValidationService userValidationService;
    
    public AccountResponse createAccount(AccountCreationRequest request) {
        log.info("Creating account for user: {}", request.getUserId());
        
        // Validate that the user exists before creating an account
        if (!userValidationService.validateUserExists(request.getUserId())) {
            throw new RuntimeException("User with ID " + request.getUserId() + " does not exist");
        }
        
        // Generate unique account number
        String accountNumber = generateAccountNumber();
        
        Account account = new Account();
        account.setUserId(request.getUserId());
        account.setAccountNumber(accountNumber);
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getInitialBalance());
        account.setStatus(AccountStatus.ACTIVE);
        
        Account savedAccount = accountRepository.save(account);
        
        return AccountResponse.builder()
                .accountId(savedAccount.getAccountId())
                .accountNumber(savedAccount.getAccountNumber())
                .accountType(savedAccount.getAccountType())
                .balance(savedAccount.getBalance())
                .status(savedAccount.getStatus())
                .message("Account created successfully.")
                .build();
    }
    
    public AccountResponse getAccount(UUID accountId) {
        log.info("Fetching account: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account with ID " + accountId + " not found."));
        
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }
    
    public List<AccountResponse> getUserAccounts(UUID userId) {
        log.info("Fetching accounts for user: {}", userId);
        
        // Validate that the user exists before fetching accounts
        if (!userValidationService.validateUserExists(userId)) {
            throw new RuntimeException("User with ID " + userId + " does not exist");
        }
        
        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.isEmpty()) {
            throw new RuntimeException("No accounts found for user: " + userId);
        }
        
        return accounts.stream()
                .map(account -> AccountResponse.builder()
                        .accountId(account.getAccountId())
                        .accountNumber(account.getAccountNumber())
                        .accountType(account.getAccountType())
                        .balance(account.getBalance())
                        .status(account.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Transactional
    public TransferResponse updateAccountBalance(TransferRequest request) {
        log.info("Updating account balances for transfer: {} -> {}, amount: {}", 
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("From account not found"));
        
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new RuntimeException("To account not found"));
        
        // Check if accounts are active
        // if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
        //     throw new RuntimeException("From account is not active");
        // }
        
        // if (toAccount.getStatus() != AccountStatus.ACTIVE) {
        //     throw new RuntimeException("To account is not active");
        // }
        
        // Check sufficient balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        log.info("Account balances updated successfully");

        return TransferResponse.builder()
                .message("Account balances updated successfully")
                .build();
    }
    
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void inactivateStaleAccounts() {
        log.info("Starting scheduled job to inactivate stale accounts");
        
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<Account> staleAccounts = accountRepository.findStaleAccounts(threshold);
        
        for (Account account : staleAccounts) {
            account.setStatus(AccountStatus.INACTIVE);
            accountRepository.save(account);
            log.info("Inactivated stale account: {}", account.getAccountId());
        }
        
        log.info("Completed inactivating {} stale accounts", staleAccounts.size());
    }
    
    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.format("%010d", (int) (Math.random() * 10000000000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        
        return accountNumber;
    }
} 
package com.example.accountservice.repository;

import com.example.accountservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    
    List<Account> findByUserId(UUID userId);
    
    @Query("SELECT a FROM Account a WHERE a.status = 'ACTIVE' AND a.lastTransactionAt < ?1")
    List<Account> findStaleAccounts(LocalDateTime threshold);
    
    boolean existsByAccountNumber(String accountNumber);
} 
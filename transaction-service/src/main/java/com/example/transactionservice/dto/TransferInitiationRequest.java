package com.example.transactionservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferInitiationRequest {
    
    @NotNull(message = "From account ID is required")
    private UUID fromAccountId;
    
    @NotNull(message = "To account ID is required")
    private UUID toAccountId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String description;
} 
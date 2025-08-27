package com.example.bffservice.controller;

import com.example.bffservice.dto.DashboardResponse;
import com.example.bffservice.model.UserAccount;
import com.example.bffservice.service.BffService;
import com.example.transactionservice.dto.TransactionResponse;
import com.example.userservice.dto.ErrorResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bff")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BffController {
    
    private final BffService bffService;

    @GetMapping("/dashboard/{userId}")
    public ResponseEntity<?> getDashboard(@PathVariable UUID userId) {
        try {
            String request = "Get /bff/dashboard/" + userId;
            bffService.sendLog(request, "Request");
            DashboardResponse response = bffService.getDashboard(userId);
            bffService.sendLog(response, "Response");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Dashboard retrieval failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(e.getMessage())
                .build();
            bffService.sendLog(response, "Response");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
} 

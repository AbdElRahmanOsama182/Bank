package com.example.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {
    
    private final WebClient webClient;
    
    @Value("${user.service.url}")
    private String userServiceUrl;
    
    /**
     * Validates that a user exists by calling the User Service
     * @param userId The user ID to validate
     * @return true if user exists, false otherwise
     */
    public Boolean validateUserExists(UUID userId) {
        try {
            log.info("Validating user existence for userId: {}", userId);
            
            // Call User Service to get user profile
            return webClient.get()
                    .uri(userServiceUrl + "/users/" + userId + "/profile")
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return Mono.just(true);
                        }
                        return Mono.just(false);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error validating user for userId: {} - {}", userId, e.getMessage());
            throw new RuntimeException("Unable to validate user existence: " + e.getMessage());
        }
    }
} 
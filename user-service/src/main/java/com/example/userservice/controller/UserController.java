package com.example.userservice.controller;

import com.example.userservice.dto.ErrorResponse;
import com.example.userservice.dto.UserLoginRequest;
import com.example.userservice.dto.UserRegistrationRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            userService.sendLog(request,"Request");
            UserResponse response = userService.registerUser(request);
            userService.sendLog(response,"Response");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.CONFLICT.value())
                    .error("Conflict")
                    .message(e.getMessage())
                    .build();
            userService.sendLog(response,"Response");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserLoginRequest request) {
        try {
            userService.sendLog(request, "Request");
            UserResponse response = userService.loginUser(request);
            userService.sendLog(response, "Response");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .error("Unauthorized")
                    .message(e.getMessage())
                    .build();
            userService.sendLog(response, "Response");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    @GetMapping("/{userId}/profile")
    public ResponseEntity<?> getUserProfile(@PathVariable UUID userId) {
        try {
            String request = "Get /users/" + userId + "/profile";
            userService.sendLog(request, "Request");
            UserResponse response = userService.getUserProfile(userId);
            userService.sendLog(response, "Response");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Profile retrieval failed: {}", e.getMessage());
            ErrorResponse response = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .error("Not Found")
                    .message(e.getMessage())
                    .build();
            userService.sendLog(response, "Response");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
} 
package org.example.logging.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.logging.model.logDump;
import org.example.logging.repository.logRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class logService {

    private final logRepository logRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "logging-topic", groupId = "logging-group")
    public void consume(String message) {
        try {
            log.info("Received message: {}", message);
            // Parse the JSON message
            JsonNode jsonNode = objectMapper.readTree(message);

            // Extract fields
            String logMessage = jsonNode.get("message").asText();
            String messageType = jsonNode.get("messageType").asText();
            String dateTime = jsonNode.get("dateTime").asText();

            // Create and save logDump entity
            logDump log = logDump.builder()
                    .id(null)
                    .message(logMessage)
                    .messageType(messageType)
                    .dateTime(dateTime)
                    .build();

            logRepository.save(log);
        } catch (Exception e) {
            // Handle parsing or database errors
            e.printStackTrace();
        }
    }
}
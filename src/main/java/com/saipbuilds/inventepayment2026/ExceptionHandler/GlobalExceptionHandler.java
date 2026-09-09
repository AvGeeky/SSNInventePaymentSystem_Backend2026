package com.saipbuilds.inventepayment2026.ExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        String errorId = java.util.UUID.randomUUID().toString();
        log.error("ID:{} An unexpected error occurred: {}", errorId, ex.getMessage());
        if (ex.getMessage().contains("Hackathon registration limit reached")){
            return ResponseEntity.status(409).body(
                    Map.of("message", ex.getMessage(),"error_id", errorId)
            );
        }
        return ResponseEntity.status(500).body(
                Map.of("message", "Something went wrong. Please try again later.","error_id", errorId)
        );
    }
}
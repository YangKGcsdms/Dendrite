package com.carter.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler for REST APIs.
 * Converts exceptions to standardized error responses.
 *
 * @author Carter
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        // Silently handle favicon.ico and other static resource 404s
        if (ex.getResourcePath().contains("favicon")) {
            return ResponseEntity.notFound().build();
        }
        
        log.debug("Resource not found: {}", ex.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "X404",
                                "message", "Resource not found: " + ex.getResourcePath()
                        ),
                        "timestamp", Instant.now().toString()
                ));
    }

    @ExceptionHandler(DendriteException.class)
    public ResponseEntity<Map<String, Object>> handleDendriteException(DendriteException ex) {
        log.warn("Business exception: {} - {}", ex.getErrorCode().getCode(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", ex.getErrorCode().getCode(),
                                "message", ex.getMessage()
                        ),
                        "timestamp", Instant.now().toString()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "X002",
                                "message", ex.getMessage()
                        ),
                        "timestamp", Instant.now().toString()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "X001",
                                "message", "An unexpected error occurred"
                        ),
                        "timestamp", Instant.now().toString()
                ));
    }
}


package com.carter.dto;

import java.time.Instant;

/**
 * Standardized API response wrapper.
 * All API endpoints should return this format for consistency.
 *
 * @param <T> the type of data payload
 * @author Carter
 * @since 1.0.0
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String timestamp
) {
    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now().toString());
    }

    /**
     * Creates a successful response with data and message.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now().toString());
    }

    /**
     * Creates an error response.
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now().toString());
    }
}


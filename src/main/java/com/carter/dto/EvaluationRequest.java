package com.carter.dto;

/**
 * Request DTO for submitting an evaluation.
 *
 * @author Carter
 * @since 1.0.0
 */
public record EvaluationRequest(
        String employee,
        String content
) {
    /**
     * Validates the request fields.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (employee == null || employee.isBlank()) {
            throw new IllegalArgumentException("Employee name is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }
        if (content.length() < 10) {
            throw new IllegalArgumentException("Content must be at least 10 characters");
        }
    }
}


package com.carter.exception;

/**
 * Base exception for Dendrite application.
 * All custom exceptions should extend this class.
 *
 * @author Carter
 * @since 1.0.0
 */
public class DendriteException extends RuntimeException {

    private final ErrorCode errorCode;

    public DendriteException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public DendriteException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
    }

    public DendriteException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Error codes for the application.
     * Each code has a unique identifier and human-readable message.
     */
    public enum ErrorCode {
        // Employee related
        EMPLOYEE_NOT_FOUND("E001", "Employee not found"),
        EMPLOYEE_NO_DATA("E002", "No evaluation data for employee"),

        // Evaluation related
        EVALUATION_EMPTY("V001", "Evaluation content is empty"),
        EVALUATION_PARSE_FAILED("V002", "Failed to parse AI response"),

        // AI related
        AI_CALL_FAILED("A001", "AI service call failed"),
        AI_RESPONSE_INVALID("A002", "Invalid AI response format"),

        // Search related
        SEARCH_FAILED("S001", "Search operation failed"),
        SEARCH_NO_RESULTS("S002", "No matching results found"),

        // System related
        INTERNAL_ERROR("X001", "Internal server error"),
        INVALID_PARAMETER("X002", "Invalid parameter");

        private final String code;
        private final String message;

        ErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}


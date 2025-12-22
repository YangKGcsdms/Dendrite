package com.carter.common;

/**
 * Application-wide constants.
 * Centralized location for all magic numbers and strings.
 *
 * @author Carter
 * @since 1.0.0
 */
public final class Constants {

    private Constants() {
        // Utility class, prevent instantiation
    }

    // ==========================================
    // Redis Keys
    // ==========================================
    public static final String REDIS_QUEUE_KEY = "dendrite:evaluation:queue";

    // ==========================================
    // Search Configuration
    // ==========================================
    public static final int DEFAULT_SEARCH_LIMIT = 5;
    public static final double SIMILARITY_THRESHOLD = 0.7;
    public static final int QUERY_CACHE_MAX_SIZE = 100;

    // ==========================================
    // Batch Processing
    // ==========================================
    public static final int MAX_BATCH_SIZE = 10;
    public static final long QUEUE_SCAN_INTERVAL_MS = 300_000; // 5 minutes
    public static final long QUEUE_INITIAL_DELAY_MS = 10_000;  // 10 seconds

    // ==========================================
    // Gamification
    // ==========================================
    public static final int POINTS_PER_LEVEL = 100;
    public static final int MAX_LEVEL = 5;
    public static final int SEARCH_HIT_REWARD = 50;
    public static final int EVALUATION_SUBMIT_REWARD = 5;
    public static final double BASE_WEIGHT = 1.0;
    public static final double LEVEL_WEIGHT_INCREMENT = 0.25;

    // ==========================================
    // AI Configuration
    // ==========================================
    public static final int VECTOR_DIMENSION = 768;
    public static final int SUMMARY_MAX_LENGTH = 200;
    public static final int MIN_SKILL_TAGS = 5;
    public static final int MAX_SKILL_TAGS = 10;

    // ==========================================
    // Validation
    // ==========================================
    public static final int MIN_EMPLOYEE_NAME_LENGTH = 1;
    public static final int MAX_EMPLOYEE_NAME_LENGTH = 100;
    public static final int MIN_CONTENT_LENGTH = 10;
    public static final int MAX_CONTENT_LENGTH = 5000;
}


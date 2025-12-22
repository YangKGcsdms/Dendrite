package com.carter.dto;

/**
 * Response DTO for queue status endpoint.
 *
 * @author Carter
 * @since 1.0.0
 */
public record QueueStatusResponse(
        long queueSize,
        String processFrequency,
        int batchSize,
        String pipelineDescription
) {
    public static QueueStatusResponse of(long size) {
        return new QueueStatusResponse(
                size,
                "Every 5 minutes",
                10,
                "Evaluation Analysis → Profile Summary → Vector Storage"
        );
    }
}


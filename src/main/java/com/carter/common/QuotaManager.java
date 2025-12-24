package com.carter.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages API quotas to prevent 429 Too Many Requests errors.
 * Specifically targets the strict rate limits of Google Vertex AI.
 */
@Component
public class QuotaManager {
    private static final Logger log = LoggerFactory.getLogger(QuotaManager.class);
    
    // Conservative limit: 1 request every 15 seconds (4 requests/min) to be safe against 5 QPM limit
    private static final long EMBEDDING_INTERVAL_MS = 15000;
    
    private final Lock embeddingLock = new ReentrantLock(true); // Fair lock ensures FIFO
    private long lastEmbeddingTime = 0;

    /**
     * Blocks until it's safe to make an embedding API call.
     */
    public void acquireEmbeddingQuota() {
        embeddingLock.lock();
        try {
            long now = System.currentTimeMillis();
            long timeSinceLast = now - lastEmbeddingTime;
            
            if (timeSinceLast < EMBEDDING_INTERVAL_MS) {
                long waitTime = EMBEDDING_INTERVAL_MS - timeSinceLast;
                log.info("Quota protection: Waiting {}ms for embedding quota (Limit: 5 QPM)", waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for quota", e);
                }
            }
            // Update time to NOW (after sleep)
            lastEmbeddingTime = System.currentTimeMillis();
        } finally {
            embeddingLock.unlock();
        }
    }
}


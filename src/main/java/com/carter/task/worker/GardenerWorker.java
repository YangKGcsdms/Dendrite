package com.carter.task.worker;

import com.carter.service.TaskProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background worker for maintenance tasks.
 * 
 * Note: Real-time processing is now handled by EvaluationProcessorService.
 * This worker only handles cleanup and maintenance tasks.
 *
 * @author Carter
 * @since 1.0.0
 */
@Component
public class GardenerWorker {

    private static final Logger log = LoggerFactory.getLogger(GardenerWorker.class);

    private final TaskProgressService progressService;

    public GardenerWorker(TaskProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * Cleanup old completed tasks every 5 minutes.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void cleanupOldTasks() {
        log.debug("[Worker] Cleaning up old completed tasks...");
        progressService.cleanupOldTasks();
    }
}

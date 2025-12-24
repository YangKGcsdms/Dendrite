package com.carter.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking evaluation task progress.
 * Enables real-time progress display in frontend.
 *
 * @author Carter
 * @since 1.0.0
 */
@Service
public class TaskProgressService {

    private final Map<String, TaskProgress> taskProgressMap = new ConcurrentHashMap<>();

    /**
     * Creates a new task and returns its ID.
     */
    public String createTask(String employeeName) {
        String taskId = "task_" + System.currentTimeMillis();
        TaskProgress progress = new TaskProgress(
                taskId,
                employeeName,
                TaskStatus.QUEUED,
                "等待处理",
                "Waiting to process",
                0,
                LocalDateTime.now(),
                null
        );
        taskProgressMap.put(taskId, progress);
        return taskId;
    }

    /**
     * Updates task progress.
     */
    public void updateProgress(String taskId, TaskStatus status, String stepZh, String stepEn, int percent) {
        TaskProgress existing = taskProgressMap.get(taskId);
        if (existing != null) {
            taskProgressMap.put(taskId, new TaskProgress(
                    taskId,
                    existing.employeeName(),
                    status,
                    stepZh,
                    stepEn,
                    percent,
                    existing.startTime(),
                    status == TaskStatus.COMPLETED || status == TaskStatus.FAILED ? LocalDateTime.now() : null
            ));
        }
    }

    /**
     * Marks task as completed.
     */
    public void completeTask(String taskId, String resultZh, String resultEn) {
        TaskProgress existing = taskProgressMap.get(taskId);
        if (existing != null) {
            taskProgressMap.put(taskId, new TaskProgress(
                    taskId,
                    existing.employeeName(),
                    TaskStatus.COMPLETED,
                    resultZh,
                    resultEn,
                    100,
                    existing.startTime(),
                    LocalDateTime.now()
            ));
        }
    }

    /**
     * Marks task as failed.
     */
    public void failTask(String taskId, String errorZh, String errorEn) {
        TaskProgress existing = taskProgressMap.get(taskId);
        if (existing != null) {
            taskProgressMap.put(taskId, new TaskProgress(
                    taskId,
                    existing.employeeName(),
                    TaskStatus.FAILED,
                    errorZh,
                    errorEn,
                    existing.percent(),
                    existing.startTime(),
                    LocalDateTime.now()
            ));
        }
    }

    /**
     * Gets current task progress.
     */
    public TaskProgress getProgress(String taskId) {
        return taskProgressMap.get(taskId);
    }

    /**
     * Cleans up old completed tasks (older than 5 minutes).
     */
    public void cleanupOldTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        taskProgressMap.entrySet().removeIf(entry -> {
            TaskProgress p = entry.getValue();
            return p.endTime() != null && p.endTime().isBefore(cutoff);
        });
    }

    // ==========================================
    // DTOs
    // ==========================================

    public enum TaskStatus {
        QUEUED,      // 排队中
        PROCESSING,  // 处理中
        COMPLETED,   // 已完成
        FAILED       // 失败
    }

    public record TaskProgress(
            String taskId,
            String employeeName,
            TaskStatus status,
            String stepZh,
            String stepEn,
            int percent,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {}
}


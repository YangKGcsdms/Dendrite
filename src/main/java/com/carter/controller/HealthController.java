package com.carter.controller;

import com.carter.dto.ApiResponse;
import com.carter.repo.TalentProfileRepository;
import com.carter.repo.SkillRecordRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Health check and system statistics endpoints.
 *
 * @author Carter
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final TalentProfileRepository profileRepo;
    private final SkillRecordRepository skillRepo;
    private final RedisTemplate<String, Object> redisTemplate;

    public HealthController(TalentProfileRepository profileRepo,
                            SkillRecordRepository skillRepo,
                            RedisTemplate<String, Object> redisTemplate) {
        this.profileRepo = profileRepo;
        this.skillRepo = skillRepo;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Basic health check for load balancers.
     */
    @GetMapping("/health")
    public ApiResponse<HealthStatus> health() {
        boolean dbHealthy = checkDatabase();
        boolean redisHealthy = checkRedis();

        String status = (dbHealthy && redisHealthy) ? "UP" : "DEGRADED";

        return ApiResponse.success(new HealthStatus(
                status,
                dbHealthy,
                redisHealthy,
                Instant.now().toString()
        ));
    }

    /**
     * System statistics for monitoring.
     */
    @GetMapping("/stats")
    public ApiResponse<SystemStats> stats() {
        long profileCount = profileRepo.count();
        long skillCount = skillRepo.count();
        Long queueSize = redisTemplate.opsForList().size("dendrite:evaluation:queue");

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;

        return ApiResponse.success(new SystemStats(
                profileCount,
                skillCount,
                queueSize != null ? queueSize : 0,
                usedMemory,
                maxMemory,
                runtime.availableProcessors()
        ));
    }

    private boolean checkDatabase() {
        try {
            profileRepo.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            redisTemplate.hasKey("health-check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public record HealthStatus(
            String status,
            boolean database,
            boolean redis,
            String timestamp
    ) {}

    public record SystemStats(
            long profileCount,
            long skillCount,
            long queueSize,
            long usedMemoryMB,
            long maxMemoryMB,
            int availableProcessors
    ) {}
}


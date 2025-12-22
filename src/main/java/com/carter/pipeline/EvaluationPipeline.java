package com.carter.pipeline;

import com.carter.entity.SkillRecord;
import com.carter.entity.TalentProfile;
import com.carter.service.GardenerService;
import com.carter.service.SummarizerService;
import com.carter.task.BatchEvaluationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Processing pipeline for batch evaluations.
 * Orchestrates: Evaluation Analysis → Profile Summary → Vector Storage
 *
 * @author Carter
 * @since 1.0.0
 */
@Component
public class EvaluationPipeline {

    private static final Logger log = LoggerFactory.getLogger(EvaluationPipeline.class);

    private final GardenerService gardenerService;
    private final SummarizerService summarizerService;

    public EvaluationPipeline(GardenerService gardenerService, SummarizerService summarizerService) {
        this.gardenerService = gardenerService;
        this.summarizerService = summarizerService;
    }

    /**
     * Executes the complete evaluation pipeline.
     *
     * @param batchTask the batch of evaluations to process
     * @return pipeline execution result
     */
    public PipelineResult execute(BatchEvaluationTask batchTask) {
        long startTime = System.currentTimeMillis();
        log.info("Pipeline started: {} evaluation tasks", batchTask.tasks().size());

        try {
            // Node 1: Extract skills from evaluations
            log.info("[Node 1] Starting skill extraction...");
            Map<String, List<SkillRecord>> skillResults = extractSkills(batchTask);
            int skillCount = skillResults.values().stream().mapToInt(List::size).sum();
            log.info("[Node 1] Complete: {} skills extracted", skillCount);

            // Node 2: Generate talent profiles
            log.info("[Node 2] Starting profile generation...");
            List<TalentProfile> profiles = generateProfiles(batchTask.getDistinctEmployees());
            log.info("[Node 2] Complete: {} profiles updated", profiles.size());

            // Node 3: Vector storage (handled in Node 2)
            log.info("[Node 3] Vector storage completed in Node 2");

            long duration = System.currentTimeMillis() - startTime;
            log.info("Pipeline completed in {}ms", duration);

            return new PipelineResult(true, skillCount, profiles.size(), profiles.size(), duration, null);

        } catch (Exception e) {
            log.error("Pipeline failed", e);
            long duration = System.currentTimeMillis() - startTime;
            return new PipelineResult(false, 0, 0, 0, duration, e.getMessage());
        }
    }

    // ==========================================
    // Pipeline Nodes
    // ==========================================

    private Map<String, List<SkillRecord>> extractSkills(BatchEvaluationTask batchTask) {
        Map<String, List<SkillRecord>> results = new HashMap<>();

        for (String employee : batchTask.getDistinctEmployees()) {
            String mergedContent = batchTask.getMergedContentFor(employee);
            log.info("[Node 1] Processing employee: {}, content length: {}", employee, mergedContent.length());

            try {
                List<SkillRecord> skills = gardenerService.processBatchEvaluation(employee, mergedContent);
                results.put(employee, skills);
                log.info("[Node 1] Employee {} - extracted {} skills", employee, skills.size());
            } catch (Exception e) {
                log.error("[Node 1] Failed to process evaluation for {}: {}", employee, e.getMessage(), e);
            }
        }

        return results;
    }

    private List<TalentProfile> generateProfiles(List<String> employees) {
        return employees.stream()
                .map(this::safeGenerateProfile)
                .filter(Objects::nonNull)
                .toList();
    }

    private TalentProfile safeGenerateProfile(String employee) {
        try {
            log.info("[Node 2] Generating profile for: {}", employee);
            TalentProfile profile = summarizerService.generateProfile(employee);
            log.info("[Node 2] Profile generated for {}: id={}, skills={}", 
                    employee, profile.getId(), profile.getTopSkills());
            return profile;
        } catch (Exception e) {
            log.error("[Node 2] Failed to generate profile for {}: {}", employee, e.getMessage(), e);
            return null;
        }
    }

    // ==========================================
    // Result Record
    // ==========================================

    /**
     * Pipeline execution result.
     *
     * @param success whether pipeline completed successfully
     * @param evaluatedCount number of skills extracted
     * @param profilesUpdated number of profiles updated
     * @param vectorsStored number of vectors stored
     * @param durationMs execution time in milliseconds
     * @param errorMessage error message if failed
     */
    public record PipelineResult(
            boolean success,
            int evaluatedCount,
            int profilesUpdated,
            int vectorsStored,
            long durationMs,
            String errorMessage
    ) {
        public boolean isSuccess() {
            return success;
        }

        public int getEvaluatedCount() {
            return evaluatedCount;
        }

        public int getProfilesUpdated() {
            return profilesUpdated;
        }

        public int getVectorsStored() {
            return vectorsStored;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

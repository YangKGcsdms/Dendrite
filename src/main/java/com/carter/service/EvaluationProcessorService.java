package com.carter.service;

import com.carter.common.QuotaManager;
import com.carter.common.VectorUtils;
import com.carter.entity.SkillRecord;
import com.carter.entity.TalentProfile;
import com.carter.repo.SkillRecordRepository;
import com.carter.service.TaskProgressService.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Real-time evaluation processor.
 * Processes evaluations immediately upon submission.
 *
 * @author Carter
 * @since 1.0.0
 */
@Service
public class EvaluationProcessorService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationProcessorService.class);

    private final GardenerService gardenerService;
    private final SummarizerService summarizerService;
    private final TaskProgressService progressService;
    private final EmbeddingModel embeddingModel;
    private final QuotaManager quotaManager;
    private final SkillRecordRepository skillRepo;
    private final JdbcTemplate jdbcTemplate;

    public EvaluationProcessorService(GardenerService gardenerService,
                                       SummarizerService summarizerService,
                                       TaskProgressService progressService,
                                       EmbeddingModel embeddingModel,
                                       QuotaManager quotaManager,
                                       SkillRecordRepository skillRepo,
                                       JdbcTemplate jdbcTemplate) {
        this.gardenerService = gardenerService;
        this.summarizerService = summarizerService;
        this.progressService = progressService;
        this.embeddingModel = embeddingModel;
        this.quotaManager = quotaManager;
        this.skillRepo = skillRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Processes an evaluation asynchronously with progress tracking.
     *
     * @param taskId task ID for progress tracking
     * @param employeeName employee name
     * @param content evaluation content
     * @return CompletableFuture with the result
     */
    @Async("taskExecutor")
    public CompletableFuture<ProcessResult> processEvaluation(String taskId, String employeeName, String content) {
        log.info("[Process] Starting evaluation for: {}, taskId: {}", employeeName, taskId);
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Extract skills (30%) - SKIP EMBEDDING
            progressService.updateProgress(taskId, TaskStatus.PROCESSING,
                    "正在提取技能...", "Extracting skills...", 10);
            
            // Pass true to skip embedding generation
            List<SkillRecord> skills = gardenerService.processEvaluation(employeeName, content, true);
            
            progressService.updateProgress(taskId, TaskStatus.PROCESSING,
                    "技能提取完成，共 " + skills.size() + " 项",
                    "Skills extracted: " + skills.size(), 30);
            
            log.info("[Process] Step 1 complete: {} skills extracted", skills.size());

            // Step 2: Generate profile (70%) - SKIP EMBEDDING
            progressService.updateProgress(taskId, TaskStatus.PROCESSING,
                    "正在生成人才画像...", "Generating talent profile...", 50);
            
            // Pass true to skip embedding generation
            TalentProfile profile = summarizerService.generateProfile(employeeName, true);
            
            progressService.updateProgress(taskId, TaskStatus.PROCESSING,
                    "人才画像生成完成", "Profile generated", 80);
            
            log.info("[Process] Step 2 complete: profile generated for {}", employeeName);

            // Step 3: Global Batch Embedding (100%)
            progressService.updateProgress(taskId, TaskStatus.PROCESSING,
                    "正在生成并保存向量(批量模式)...", "Generating vectors (Batch Mode)...", 90);

            generateAndSaveVectors(skills, profile);

            long duration = System.currentTimeMillis() - startTime;
            
            progressService.completeTask(taskId,
                    String.format("处理完成！提取 %d 项技能，耗时 %dms", skills.size(), duration),
                    String.format("Complete! %d skills extracted in %dms", skills.size(), duration));

            log.info("[Process] Evaluation completed for {} in {}ms", employeeName, duration);

            return CompletableFuture.completedFuture(new ProcessResult(
                    true, employeeName, skills.size(), duration, null
            ));

        } catch (Exception e) {
            log.error("[Process] Failed to process evaluation for {}: {}", employeeName, e.getMessage(), e);
            
            progressService.failTask(taskId,
                    "处理失败: " + e.getMessage(),
                    "Process failed: " + e.getMessage());

            return CompletableFuture.completedFuture(new ProcessResult(
                    false, employeeName, 0, 0, e.getMessage()
            ));
        }
    }

    private void generateAndSaveVectors(List<SkillRecord> skills, TalentProfile profile) {
        // Collect all texts: Skill 1, Skill 2, ..., Skill N, Profile Summary
        List<String> allTexts = new ArrayList<>();
        
        // 1. Add skill texts
        for (SkillRecord skill : skills) {
            allTexts.add(skill.getSkillName() + ": " + skill.getEvidence());
        }
        
        // 2. Add profile text
        String profileText = "";
        if (profile.getSummaryEn() != null) {
            String tags = profile.getSkillsEn() != null ? String.join(", ", profile.getSkillsEn()) : "";
            profileText = profile.getSummaryEn() + " " + tags;
        }
        allTexts.add(profileText); // Always add to keep index alignment

        if (allTexts.isEmpty()) {
            return;
        }

        try {
            // SINGLE API CALL for everything
            log.info("[Process] Generating vectors for {} items in one batch", allTexts.size());
            quotaManager.acquireEmbeddingQuota();
            
            EmbeddingResponse response = embeddingModel.call(
                new EmbeddingRequest(allTexts, null)
            );
            
            List<List<Double>> vectors = response.getResults().stream()
                    .map(emb -> VectorUtils.toDoubleList(emb.getOutput()))
                    .toList();

            // Distribute vectors back
            // A. Skills
            for (int i = 0; i < skills.size(); i++) {
                if (i < vectors.size()) {
                    skills.get(i).setEmbedding(vectors.get(i));
                }
            }
            skillRepo.saveAll(skills); // Update with vectors

            // B. Profile
            int profileIndex = allTexts.size() - 1; // Last one
            if (profileIndex < vectors.size() && !profileText.isEmpty()) {
                List<Double> profileVector = vectors.get(profileIndex);
                
                // Fix: Properly formatted string for PGVector
                String vectorStr = VectorUtils.toVectorString(profileVector);
                
                jdbcTemplate.update("UPDATE dendrite_profiles SET embedding = ?::vector WHERE id = ?", 
                        vectorStr, profile.getId());
                log.info("[Process] Profile vector updated");
            }

        } catch (Exception e) {
            log.error("[Process] Failed to generate batch vectors", e);
            // Don't fail the whole process, just log error. 
            // Data is saved without vectors, can be retried later.
        }
    }

    /**
     * Result of evaluation processing.
     */
    public record ProcessResult(
            boolean success,
            String employeeName,
            int skillCount,
            long durationMs,
            String error
    ) {}
}


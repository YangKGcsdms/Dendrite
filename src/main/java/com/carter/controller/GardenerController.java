package com.carter.controller;

import com.carter.common.Constants;
import com.carter.dto.ApiResponse;
import com.carter.dto.EvaluationRequest;
import com.carter.dto.QueueStatusResponse;
import com.carter.dto.SearchResultDto;
import com.carter.entity.TalentProfile;
import com.carter.pipeline.EvaluationPipeline;
import com.carter.service.SearchService;
import com.carter.service.SummarizerService;
import com.carter.service.TokenUsageTracker;
import com.carter.task.EvaluationTask;
import com.carter.task.worker.GardenerWorker;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for the Dendrite talent management system.
 * Provides endpoints for evaluation submission, talent search, and system management.
 *
 * @author Carter
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/gardener")
public class GardenerController {

    private final SearchService searchService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SummarizerService summarizerService;
    private final GardenerWorker gardenerWorker;
    private final TokenUsageTracker tokenTracker;

    public GardenerController(RedisTemplate<String, Object> redisTemplate,
                              SummarizerService summarizerService,
                              SearchService searchService,
                              GardenerWorker gardenerWorker,
                              TokenUsageTracker tokenTracker) {
        this.redisTemplate = redisTemplate;
        this.summarizerService = summarizerService;
        this.searchService = searchService;
        this.gardenerWorker = gardenerWorker;
        this.tokenTracker = tokenTracker;
    }

    // ==========================================
    // Evaluation Endpoints
    // ==========================================

    /**
     * Submits a single evaluation to the processing queue.
     *
     * @param employee the employee being evaluated
     * @param content the evaluation content
     * @return submission confirmation
     */
    @PostMapping("/evaluate")
    public ApiResponse<EvaluationSubmitResult> submitEvaluation(
            @RequestParam String employee,
            @RequestBody String content) {

        EvaluationTask task = new EvaluationTask(employee, content);
        redisTemplate.opsForList().leftPush(Constants.REDIS_QUEUE_KEY, task);

        Long queueSize = gardenerWorker.getQueueSize();

        return ApiResponse.success(
                new EvaluationSubmitResult(employee, "queued", queueSize),
                "Evaluation queued for processing"
        );
    }

    /**
     * Submits multiple evaluations in batch.
     *
     * @param evaluations list of evaluation requests
     * @return batch submission result
     */
    @PostMapping("/evaluate/batch")
    public ApiResponse<BatchSubmitResult> submitBatchEvaluations(
            @RequestBody List<EvaluationRequest> evaluations) {

        int count = 0;
        for (EvaluationRequest eval : evaluations) {
            eval.validate(); // Manual validation
            EvaluationTask task = new EvaluationTask(eval.employee(), eval.content());
            redisTemplate.opsForList().leftPush(Constants.REDIS_QUEUE_KEY, task);
            count++;
        }

        return ApiResponse.success(
                new BatchSubmitResult(count, gardenerWorker.getQueueSize()),
                String.format("Submitted %d evaluations", count)
        );
    }

    // ==========================================
    // Profile Endpoints
    // ==========================================

    /**
     * Manually triggers profile generation for an employee.
     *
     * @param employee the employee name
     * @return generated profile
     */
    @PostMapping("/summarize")
    public ApiResponse<TalentProfile> summarizeEmployee(@RequestParam String employee) {
        TalentProfile profile = summarizerService.generateProfile(employee);
        return ApiResponse.success(profile, "Profile generated successfully");
    }

    // ==========================================
    // Search Endpoints
    // ==========================================

    /**
     * Performs semantic search for talent profiles.
     *
     * @param query search query
     * @return matching profiles
     */
    @GetMapping("/search")
    public ApiResponse<List<SearchResultDto>> search(@RequestParam String query) {
        List<SearchResultDto> results = searchService.searchSimilarProfiles(
                query, Constants.DEFAULT_SEARCH_LIMIT);
        return ApiResponse.success(results);
    }

    /**
     * Performs concurrent batch search.
     *
     * @param queries list of search queries
     * @param limit results per query
     * @return search results for each query
     */
    @PostMapping("/search/batch")
    public ApiResponse<List<SearchService.BatchSearchResult>> batchSearch(
            @RequestBody List<String> queries,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(searchService.batchSearch(queries, limit));
    }

    /**
     * AI-powered talent recommendation.
     *
     * @param query the requirement description
     * @return AI recommendation
     */
    @GetMapping("/ask")
    public ApiResponse<String> ask(@RequestParam String query) {
        String answer = searchService.searchAndRecommend(query);
        return ApiResponse.success(answer);
    }

    /**
     * Concurrent batch AI recommendations.
     *
     * @param queries list of questions
     * @return AI answers for each query
     */
    @PostMapping("/ask/batch")
    public ApiResponse<List<SearchService.BatchAskResult>> batchAsk(@RequestBody List<String> queries) {
        return ApiResponse.success(searchService.batchAsk(queries));
    }

    // ==========================================
    // Management Endpoints
    // ==========================================

    /**
     * Returns current queue status.
     */
    @GetMapping("/queue/status")
    public ApiResponse<QueueStatusResponse> getQueueStatus() {
        Long size = gardenerWorker.getQueueSize();
        return ApiResponse.success(QueueStatusResponse.of(size != null ? size : 0));
    }

    /**
     * Manually triggers queue processing (for testing).
     */
    @PostMapping("/queue/process")
    public ApiResponse<PipelineExecutionResult> manualProcessQueue() {
        EvaluationPipeline.PipelineResult result = gardenerWorker.triggerManualProcess();

        if (result == null) {
            return ApiResponse.success(null, "Queue is empty, nothing to process");
        }

        return ApiResponse.success(new PipelineExecutionResult(
                result.isSuccess(),
                result.getEvaluatedCount(),
                result.getProfilesUpdated(),
                result.getVectorsStored(),
                result.getDurationMs(),
                result.getErrorMessage()
        ));
    }

    // ==========================================
    // Token Monitoring
    // ==========================================

    /**
     * Returns token usage report.
     */
    @GetMapping("/token/report")
    public ApiResponse<TokenUsageTracker.UsageReport> getTokenReport() {
        return ApiResponse.success(tokenTracker.getReport());
    }

    /**
     * Resets token statistics.
     */
    @PostMapping("/token/reset")
    public ApiResponse<Void> resetTokenStats() {
        tokenTracker.reset();
        return ApiResponse.success(null, "Token statistics reset");
    }

    /**
     * Toggles economy mode.
     *
     * @param economyMode true to enable economy mode
     * @return current settings
     */
    @PostMapping("/cost-mode")
    public ApiResponse<CostModeResult> setCostMode(@RequestParam boolean economyMode) {
        searchService.setEnableQueryExpansion(!economyMode);
        return ApiResponse.success(
                new CostModeResult(economyMode, !economyMode),
                economyMode
                        ? "Economy mode enabled: Query expansion disabled"
                        : "Performance mode enabled: Query expansion active"
        );
    }

    // ==========================================
    // Response DTOs
    // ==========================================

    public record EvaluationSubmitResult(String employee, String status, Long queuePosition) {}
    public record BatchSubmitResult(int submitted, Long totalQueued) {}
    public record PipelineExecutionResult(
            boolean success, int skillsExtracted, int profilesUpdated,
            int vectorsStored, long durationMs, String error) {}
    public record CostModeResult(boolean economyMode, boolean queryExpansion) {}
}

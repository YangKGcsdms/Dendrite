package com.carter.controller;

import com.carter.common.Constants;
import com.carter.dto.ApiResponse;
import com.carter.dto.EvaluationRequest;
import com.carter.dto.SearchResultDto;
import com.carter.entity.TalentProfile;
import com.carter.service.EvaluationProcessorService;
import com.carter.service.SearchService;
import com.carter.service.SummarizerService;
import com.carter.service.TaskProgressService;
import com.carter.service.TaskProgressService.TaskProgress;
import com.carter.service.TokenUsageTracker;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    private final SummarizerService summarizerService;
    private final TokenUsageTracker tokenTracker;
    private final EvaluationProcessorService processorService;
    private final TaskProgressService progressService;

    public GardenerController(SummarizerService summarizerService,
                              SearchService searchService,
                              TokenUsageTracker tokenTracker,
                              EvaluationProcessorService processorService,
                              TaskProgressService progressService) {
        this.summarizerService = summarizerService;
        this.searchService = searchService;
        this.tokenTracker = tokenTracker;
        this.processorService = processorService;
        this.progressService = progressService;
    }

    // ==========================================
    // Evaluation Endpoints (Real-time Processing)
    // ==========================================

    /**
     * Submits an evaluation and processes it immediately.
     * Returns a task ID for progress tracking.
     *
     * @param employee the employee being evaluated
     * @param content the evaluation content
     * @return task ID for progress tracking
     */
    @PostMapping("/evaluate")
    public ApiResponse<EvaluationSubmitResult> submitEvaluation(
            @RequestParam String employee,
            @RequestBody String content) {

        // Create task and start processing immediately
        String taskId = progressService.createTask(employee);
        
        // Process asynchronously
        processorService.processEvaluation(taskId, employee, content);

        return ApiResponse.success(
                new EvaluationSubmitResult(taskId, employee, "processing"),
                "Evaluation submitted, processing started"
        );
    }

    /**
     * Gets the progress of a task.
     *
     * @param taskId task ID
     * @return current progress
     */
    @GetMapping("/task/{taskId}")
    public ApiResponse<TaskProgress> getTaskProgress(@PathVariable String taskId) {
        TaskProgress progress = progressService.getProgress(taskId);
        if (progress == null) {
            return ApiResponse.error("Task not found: " + taskId);
        }
        return ApiResponse.success(progress);
    }

    /**
     * Submits multiple evaluations and processes them.
     *
     * @param evaluations list of evaluation requests
     * @return list of task IDs
     */
    @PostMapping("/evaluate/batch")
    public ApiResponse<BatchSubmitResult> submitBatchEvaluations(
            @RequestBody List<EvaluationRequest> evaluations) {

        List<String> taskIds = new ArrayList<>();
        
        for (EvaluationRequest eval : evaluations) {
            eval.validate();
            String taskId = progressService.createTask(eval.employee());
            processorService.processEvaluation(taskId, eval.employee(), eval.content());
            taskIds.add(taskId);
        }

        return ApiResponse.success(
                new BatchSubmitResult(taskIds.size(), taskIds),
                String.format("Submitted %d evaluations for processing", taskIds.size())
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

    public record EvaluationSubmitResult(String taskId, String employee, String status) {}
    public record BatchSubmitResult(int submitted, List<String> taskIds) {}
    public record CostModeResult(boolean economyMode, boolean queryExpansion) {}
}

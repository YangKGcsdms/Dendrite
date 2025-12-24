package com.carter.service;

import com.carter.common.Constants;
import com.carter.common.VectorUtils;
import com.carter.dto.SearchResultDto;
import com.carter.entity.EvaluationTag;
import com.carter.repo.EvaluationTagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Service for semantic search and AI-powered recommendations.
 * Provides vector similarity search with optional query expansion.
 *
 * @author Carter
 * @since 1.0.0
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private static final String SEARCH_SQL = """
            SELECT employee_name, professional_summary,
                   1 - (embedding <=> ?::vector) as similarity
            FROM dendrite_profiles
            ORDER BY similarity DESC
            LIMIT ?
            """;

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final ChatClient chatClient;
    private final EvaluationTagRepository tagRepo;
    private final RewardService rewardService;
    private final com.carter.common.QuotaManager quotaManager;

    private final Map<String, String> queryExpansionCache = new ConcurrentHashMap<>();
    private volatile boolean enableQueryExpansion = true;

    public SearchService(EmbeddingModel embeddingModel,
                         JdbcTemplate jdbcTemplate,
                         ChatClient.Builder builder,
                         EvaluationTagRepository tagRepo,
                         RewardService rewardService,
                         com.carter.common.QuotaManager quotaManager) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
        this.chatClient = builder.build();
        this.tagRepo = tagRepo;
        this.rewardService = rewardService;
        this.quotaManager = quotaManager;
    }

    /**
     * Enables or disables query expansion.
     * Disabling saves 50% of AI calls but may reduce search quality.
     *
     * @param enable true to enable, false to disable
     */
    public void setEnableQueryExpansion(boolean enable) {
        this.enableQueryExpansion = enable;
        log.info("Query expansion {}", enable ? "enabled" : "disabled");
    }

    /**
     * Searches for similar talent profiles using vector similarity.
     *
     * @param queryText the search query
     * @param limit maximum number of results
     * @return list of matching profiles with similarity scores
     */
    public List<SearchResultDto> searchSimilarProfiles(String queryText, int limit) {
        quotaManager.acquireEmbeddingQuota();
        float[] queryVector = embeddingModel.embed(queryText);

        List<Map<String, Object>> rawResults = jdbcTemplate.queryForList(SEARCH_SQL, queryVector, limit);

        return rawResults.stream()
                .map(row -> new SearchResultDto(
                        (String) row.get("employee_name"),
                        (String) row.get("professional_summary"),
                        ((Number) row.get("similarity")).doubleValue()
                ))
                .toList();
    }

    /**
     * Performs semantic search with AI-powered recommendations.
     * Optionally expands the query using AI before searching.
     *
     * @param queryText the user's search query
     * @return AI-generated recommendation text
     */
    public String searchAndRecommend(String queryText) {
        String expandedQuery = getExpandedQuery(queryText);

        quotaManager.acquireEmbeddingQuota();
        float[] queryVector = embeddingModel.embed(expandedQuery);
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList(
                SEARCH_SQL, queryVector, Constants.DEFAULT_SEARCH_LIMIT);

        if (candidates.isEmpty()) {
            return "Sorry, no matching talent found in the knowledge base.";
        }

        return generateRecommendation(queryText, candidates);
    }

    /**
     * Tracks when a search result is selected, rewarding contributing taggers.
     *
     * @param query the original search query
     * @param selectedEmployeeName the selected employee
     */
    public void trackSearchHit(String query, String selectedEmployeeName) {
        List<EvaluationTag> tags = tagRepo.findByTargetEmployee(selectedEmployeeName);
        if (tags.isEmpty()) {
            return;
        }

        quotaManager.acquireEmbeddingQuota();
        float[] queryVector = embeddingModel.embed(query);

        for (EvaluationTag tag : tags) {
            List<Double> tagVector = tag.getVector();
            if (tagVector == null || tagVector.isEmpty()) {
                continue;
            }

            double similarity = VectorUtils.cosineSimilarity(queryVector, tagVector);

            if (similarity > Constants.SIMILARITY_THRESHOLD) {
                rewardService.addPoints(
                        tag.getCreatorEmployee(),
                        Constants.SEARCH_HIT_REWARD,
                        "Search assist: Your tag helped find " + selectedEmployeeName
                );
                log.info("Rewarded {} for search assist", tag.getCreatorEmployee());
            }
        }
    }

    // ==========================================
    // Batch Operations
    // ==========================================

    /**
     * Performs concurrent batch search for multiple queries.
     *
     * @param queries list of search queries
     * @param limitPerQuery maximum results per query
     * @return list of search results for each query
     */
    public List<BatchSearchResult> batchSearch(List<String> queries, int limitPerQuery) {
        List<CompletableFuture<BatchSearchResult>> futures = queries.stream()
                .map(query -> searchAsync(query, limitPerQuery))
                .toList();

        return futures.stream()
                .map(this::getResult)
                .toList();
    }

    /**
     * Performs concurrent batch AI recommendations for multiple queries.
     *
     * @param queries list of questions
     * @return list of AI answers for each query
     */
    public List<BatchAskResult> batchAsk(List<String> queries) {
        List<CompletableFuture<BatchAskResult>> futures = queries.stream()
                .map(this::askAsync)
                .toList();

        return futures.stream()
                .map(this::getAskResult)
                .toList();
    }

    @Async("searchExecutor")
    public CompletableFuture<BatchSearchResult> searchAsync(String query, int limit) {
        try {
            List<SearchResultDto> results = searchSimilarProfiles(query, limit);
            return CompletableFuture.completedFuture(new BatchSearchResult(query, results, null));
        } catch (Exception e) {
            log.error("Async search failed for query: {}", query, e);
            return CompletableFuture.completedFuture(new BatchSearchResult(query, List.of(), e.getMessage()));
        }
    }

    @Async("searchExecutor")
    public CompletableFuture<BatchAskResult> askAsync(String query) {
        try {
            String answer = searchAndRecommend(query);
            return CompletableFuture.completedFuture(new BatchAskResult(query, answer));
        } catch (Exception e) {
            log.error("Async ask failed for query: {}", query, e);
            return CompletableFuture.completedFuture(new BatchAskResult(query, "Processing failed: " + e.getMessage()));
        }
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    private String getExpandedQuery(String queryText) {
        if (!enableQueryExpansion) {
            log.debug("Economy mode: using original query");
            return queryText;
        }

        String expanded = queryExpansionCache.computeIfAbsent(queryText, this::expandQuery);
        log.debug("Query expansion: '{}' -> '{}'", queryText, expanded);

        if (queryExpansionCache.size() > Constants.QUERY_CACHE_MAX_SIZE) {
            queryExpansionCache.clear();
            log.debug("Query cache cleared (exceeded max size)");
        }

        return expanded;
    }

    private String expandQuery(String originalQuery) {
        String prompt = """
                你是搜索增强助手。用户的搜索词是："%s"

                请提取核心意图，并添加3-5个相关的专业术语或同义词，
                用于在人才库中进行向量搜索。

                示例：
                输入："会k8s"
                输出：Kubernetes、容器编排、Docker、云原生、集群运维

                输入："电脑坏了"
                输出：IT支持、桌面运维、硬件维修、系统修复

                只输出扩展后的关键词，用逗号分隔，不要其他内容。
                """.formatted(originalQuery);

        return chatClient.prompt(prompt).call().content();
    }

    private String generateRecommendation(String queryText, List<Map<String, Object>> candidates) {
        StringBuilder context = new StringBuilder();
        for (Map<String, Object> candidate : candidates) {
            context.append(String.format("- 姓名：%s，简介：%s\n",
                    candidate.get("employee_name"),
                    candidate.get("professional_summary")));
        }

        String prompt = """
                用户需求："%s"

                数据库中找到以下候选人：
                %s

                请分析推荐：
                1. 分析谁最能解决用户的问题（区分前端/后端/运维/硬件等领域）
                2. 如果有匹配的人选，直接推荐并简述理由
                3. 如果没有合适人选，诚实回答"暂未找到合适人选"
                4. 用中文回复，简洁明了

                直接给出推荐结果。
                """.formatted(queryText, context);

        return chatClient.prompt(prompt).call().content();
    }

    private BatchSearchResult getResult(CompletableFuture<BatchSearchResult> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return new BatchSearchResult(null, List.of(), "Search failed: " + e.getMessage());
        }
    }

    private BatchAskResult getAskResult(CompletableFuture<BatchAskResult> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return new BatchAskResult(null, "Ask failed: " + e.getMessage());
        }
    }

    // ==========================================
    // Result DTOs
    // ==========================================

    public record BatchSearchResult(String query, List<SearchResultDto> results, String error) {
        public boolean isSuccess() {
            return error == null;
        }
    }

    public record BatchAskResult(String query, String answer) {}
}

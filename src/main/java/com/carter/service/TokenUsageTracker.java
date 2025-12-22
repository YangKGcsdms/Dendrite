package com.carter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token 使用量追踪器
 * 帮助监控 AI 调用成本
 */
@Service
public class TokenUsageTracker {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageTracker.class);

    // 价格配置 (单位: 美元 / 1M tokens)
    private static final Map<String, double[]> MODEL_PRICES = Map.of(
            "gemini-2.5-pro", new double[]{1.25, 10.0},      // [输入, 输出]
            "gemini-2.0-flash", new double[]{0.10, 0.40},
            "gemini-1.5-flash", new double[]{0.075, 0.30},
            "text-embedding-004", new double[]{0.00, 0.00}   // Embedding 单独计费
    );

    // 当前配置的模型
    private String currentModel = "gemini-2.0-flash";

    // 累计 Token 统计
    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);
    private final AtomicLong totalEmbeddingTokens = new AtomicLong(0);

    // 按操作类型统计调用次数
    private final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();

    /**
     * 记录一次 Chat 调用
     * @param operation 操作类型 (如 "evaluate", "summarize", "ask")
     * @param inputTokens 输入 token 数 (可估算)
     * @param outputTokens 输出 token 数 (可估算)
     */
    public void recordChatUsage(String operation, long inputTokens, long outputTokens) {
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
        operationCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();

        log.debug("📊 [Token] {} - 输入: {}, 输出: {}", operation, inputTokens, outputTokens);
    }

    /**
     * 记录 Embedding 调用
     */
    public void recordEmbeddingUsage(long tokens) {
        totalEmbeddingTokens.addAndGet(tokens);
    }

    /**
     * 估算当前累计成本
     */
    public double estimateTotalCost() {
        double[] prices = MODEL_PRICES.getOrDefault(currentModel, new double[]{0.10, 0.40});

        double inputCost = (totalInputTokens.get() / 1_000_000.0) * prices[0];
        double outputCost = (totalOutputTokens.get() / 1_000_000.0) * prices[1];

        return inputCost + outputCost;
    }

    /**
     * 获取使用报告
     */
    public UsageReport getReport() {
        return new UsageReport(
                currentModel,
                totalInputTokens.get(),
                totalOutputTokens.get(),
                totalEmbeddingTokens.get(),
                estimateTotalCost(),
                Map.copyOf(operationCounts.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().get()
                        )))
        );
    }

    /**
     * 重置统计
     */
    public void reset() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalEmbeddingTokens.set(0);
        operationCounts.clear();
        log.info("📊 [Token] 统计已重置");
    }

    public void setCurrentModel(String model) {
        this.currentModel = model;
    }

    /**
     * 使用报告 DTO
     */
    public record UsageReport(
            String model,
            long inputTokens,
            long outputTokens,
            long embeddingTokens,
            double estimatedCostUSD,
            Map<String, Long> operationCounts
    ) {}

    // ==========================================
    // 便捷估算方法
    // ==========================================

    /**
     * 根据文本长度估算 Token 数
     * 粗略估算：1 中文字 ≈ 2 tokens, 1 英文单词 ≈ 1.3 tokens
     */
    public static long estimateTokens(String text) {
        if (text == null) return 0;

        // 简单估算：字符数 / 2 (对中英混合文本比较准)
        return text.length() / 2;
    }
}


package com.carter.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private static final double MIN_SCORE_THRESHOLD = 0.50;

    public SearchService(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> searchSimilarProfiles(String queryText, int limit) {
        float[] queryVector = embeddingModel.embed(queryText);

        String sql = """
                SELECT employee_name, professional_summary, 
                       1 - (embedding <=> ?::vector) as similarity
                FROM dendrite_profiles
                ORDER BY similarity DESC
                LIMIT ?
                """;

        List<Map<String, Object>> rawResults = jdbcTemplate.queryForList(sql, queryVector, limit);

        // ✅ 新增过滤逻辑 (Java 层过滤)
        return rawResults.stream()
//                .filter(row -> {
//                    // 获取相似度分数 (注意：JDBC 返回的可能是 Double 或 BigDecimal)
//                    Number simObj = (Number) row.get("similarity");
//                    double score = simObj.doubleValue();
//
//                    // 策略：如果分数低于阈值，直接扔掉
//                    return score > MIN_SCORE_THRESHOLD;
//                })
                .toList();
    }
}
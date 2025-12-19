package com.carter.service;

import com.carter.entity.EvaluationTag;
import com.carter.repo.EvaluationTagRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final ChatClient chatClient; // 👈 注入 ChatClient

    // 在 SearchService.java 中注入
    private final EvaluationTagRepository tagRepo;
    private final RewardService rewardService;

    public SearchService(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate,ChatClient.Builder builder,
                         EvaluationTagRepository tagRepo, RewardService rewardService) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
        this.chatClient = builder.build();
        this.tagRepo = tagRepo;
        this.rewardService = rewardService;
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

    public String searchAndRecommend(String queryText) {
        // ✅ 第一步：查询改写 (Query Expansion)
        // 不直接搜 queryText，而是搜 expandedQuery
        String expandedQuery = expandQuery(queryText);
        System.out.println("🔍 [Debug] 用户搜: " + queryText);
        System.out.println("🧠 [Debug] AI 改写: " + expandedQuery);

        // ✅ 第二步：用改写后的词生成向量
        float[] queryVector = embeddingModel.embed(expandedQuery);

        String sql = """
                SELECT employee_name, professional_summary, 
                       1 - (embedding <=> ?::vector) as similarity
                FROM dendrite_profiles
                ORDER BY similarity DESC
                LIMIT 5
                """;
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList(sql, queryVector);

        if (candidates.isEmpty()) {
            return "抱歉，知识库里没有找到相关人员。";
        }

        // 2. 【重排序】 构造 Prompt，让 AI 也就是 Gemini Pro 来做最终决策
        StringBuilder candidatesContext = new StringBuilder();
        for (Map<String, Object> candidate : candidates) {
            candidatesContext.append(String.format("- 姓名: %s, 简介: %s\n",
                    candidate.get("employee_name"), candidate.get("professional_summary")));
        }

        String prompt = """
                用户的需求是: "%s"
                
                我们通过数据库检索到了以下候选人：
                %s
                
                请运用你的逻辑推理能力：
                1. 分析谁最能解决用户的问题？(注意区分硬件/软件/运维/前端等领域)
                2. 如果有人选，请直接推荐并简述理由。
                3. 如果所有人都不匹配，请诚实回答“找不到合适的人”。
                
                请只返回推荐结果，不要啰嗦。
                """.formatted(queryText, candidatesContext.toString());

        // 3. 调用 AI 获取最终答案
        return chatClient.prompt(prompt).call().content();
    }

    // 私有方法：让 AI 帮忙扩充关键词
    private String expandQuery(String originalQuery) {
        String prompt = """
            你是一个搜索增强助手。用户的原始搜索词是: "%s"
            
            请提取该搜索词的核心意图，并补充 3-5 个相关的专业技术术语或同义词，以便在员工简历库中进行向量检索。
            
            示例:
            输入: "找个懂 k8s 的"
            输出: Kubernetes, 容器编排, Docker, Helm, 云原生, 集群运维
            
            输入: "电脑蓝屏了"
            输出: IT支持, 桌面运维, 硬件故障, 操作系统修复, Windows Troubleshooting
            
            请直接输出扩充后的关键词字符串，用逗号分隔，不要包含其他废话。
            """.formatted(originalQuery);

        return chatClient.prompt(prompt).call().content();
    }

    /**
     * 新增方法：搜索命中反馈 (最好是异步的 @Async)
     */
    public void trackSearchHit(String query, String selectedEmployeeName) {
        // 1. 假设用户点击了 selectedEmployeeName，我们去看看是谁的标签起了作用
        List<EvaluationTag> tags = tagRepo.findByTargetEmployee(selectedEmployeeName);
        if (tags.isEmpty()) return;

        // 2. 计算 Query 和 Tag 的相似度
        float[] queryVectorFloat = embeddingModel.embed(query);

        // 简单暴力：遍历所有 tag 计算余弦相似度
        for (EvaluationTag tag : tags) {
            List<Double> tagVector = tag.getVector();
            if (tagVector == null || tagVector.isEmpty()) continue;
            
            double similarity = cosineSimilarity(queryVectorFloat, tagVector);
            
            // 阈值判定：如果相似度 > 0.7，认为是这个标签立了大功
            if (similarity > 0.7) {
                rewardService.addPoints(tag.getCreatorEmployee(), 50, "搜索助攻: 你的标签帮助找到了 " + selectedEmployeeName);
                System.out.println("💰 已给 " + tag.getCreatorEmployee() + " 发放搜索助攻奖励！");
            }
        }
    }

    // 辅助方法：余弦相似度
    private double cosineSimilarity(float[] vec1, List<Double> vec2) {
        if (vec1.length != vec2.size()) return 0.0;
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2.get(i);
            normA += vec1[i] * vec1[i];
            normB += vec2.get(i) * vec2.get(i);
        }
        
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

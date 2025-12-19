package com.carter.service;

import com.carter.entity.ContributorProfile;
import com.carter.entity.EvaluationTag;
import com.carter.entity.SkillRecord;
import com.carter.entity.enums.StandardCompetency;
import com.carter.repo.ContributorProfileRepository;
import com.carter.repo.EvaluationTagRepository;
import com.carter.repo.SkillRecordRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carter
 * @date 2025/12/16
 * @description
 */
@Service
public class GardenerService {

    private final ChatClient chatClient;
    private final SkillRecordRepository skillRepository;
    private final EvaluationTagRepository tagRepo;
    private final ContributorProfileRepository contributorRepo;
    private final RewardService rewardService;
    private final EmbeddingModel embeddingModel;

    public GardenerService(ChatClient.Builder builder, 
                           SkillRecordRepository skillRepository,
                           EvaluationTagRepository tagRepo,
                           ContributorProfileRepository contributorRepo,
                           RewardService rewardService,
                           EmbeddingModel embeddingModel) {
        this.chatClient = builder.build();
        this.skillRepository = skillRepository;
        this.tagRepo = tagRepo;
        this.contributorRepo = contributorRepo;
        this.rewardService = rewardService;
        this.embeddingModel = embeddingModel;
    }

    // 定义一个简单的内部 Record 来接收 AI 的 JSON 结果
    public record SkillExtractionResult(String skillName, String proficiency, String evidence) {}
    public record AiResponse(List<SkillExtractionResult> skills) {}

    /**
     * 核心方法：园丁修剪
     * 输入：同事的胡言乱语
     * 输出：整齐的技能树
     */
    public List<SkillRecord> processEvaluation(String targetEmployee, String rawText) {
        // 1. 创建转换器，告诉 AI 我们要什么格式
        var converter = new BeanOutputConverter<>(AiResponse.class);

        // 2. 构造 Prompt
        String promptText = """
                你是一个专业的企业人才分析师。请分析下面的员工评价文本。
                目标员工: %s
                评价内容: "%s"
                
                请提取出该员工展现出的【技术技能】或【软技能】。
                对于每个技能，请判断熟练度（初级/中级/高级/专家），并摘录原文作为证据。
                
                %s
                """.formatted(targetEmployee, rawText, converter.getFormat()); // 👈 这一行会自动注入 JSON Schema

        // 3. 调用 AI (Gemini Pro)
        String response = chatClient.prompt(promptText).call().content();

        // 4. 解析结果 (String -> Java Object)
        AiResponse aiData = converter.convert(response);

        // 5. 存入数据库
        if (aiData != null && aiData.skills() != null) {
            List<SkillRecord> records = aiData.skills().stream().map(s -> {
                SkillRecord record = new SkillRecord();
                record.setEmployeeName(targetEmployee);
                record.setSkillName(s.skillName());
                record.setProficiency(s.proficiency());
                record.setEvidence(s.evidence());
                
                // ✅ 修复：直接生成向量，防止 null 报错
                String textToEmbed = s.skillName() + ": " + s.evidence();
                float[] vectorFloat = embeddingModel.embed(textToEmbed);
                
                // 转换 float[] 到 List<Double> 以兼容 Hibernate Mapping
                List<Double> embeddingList = new ArrayList<>();
                for (float v : vectorFloat) {
                    embeddingList.add((double) v);
                }
                record.setEmbedding(embeddingList);
                
                return record;
            }).toList();

            return skillRepository.saveAll(records);
        }
        return List.of();
    }

    /**
     * 新增方法：处理员工打标签
     */
    public void processUserTag(String fromUser, String targetUser, String rawTag, String context) {
        // 1. 获取评价者的等级权重
        ContributorProfile contributor = contributorRepo.findByEmployeeName(fromUser)
                .orElseGet(() -> {
                    // 如果是新用户，先给他建个档
                    rewardService.addPoints(fromUser, 0, "账号初始化");
                    return contributorRepo.findByEmployeeName(fromUser).orElseThrow();
                });

        // 等级越高，权重越大 (Lv1=1.0, Lv5=2.0)
        double weight = 1.0 + (contributor.getLevel() - 1) * 0.25;

        // 2. 调用 AI 进行分类
        String prompt = """
                请将以下评价标签归类为标准能力模型中的一项：
                标签: "%s"
                语境: "%s"
                
                标准能力列表:
                %s
                
                请直接返回枚举值，不要其他废话。
                """.formatted(rawTag, context, java.util.Arrays.toString(StandardCompetency.values()));
        
        String categoryStr = chatClient.prompt(prompt).call().content();
        StandardCompetency category;
        try {
             // AI 可能返回一些 markdown 或空格，清理一下
             String cleanCategory = categoryStr.replaceAll("[^a-zA-Z_]", "").toUpperCase();
             category = StandardCompetency.valueOf(cleanCategory);
        } catch (Exception e) {
            category = StandardCompetency.HARD_SKILL_GENERAL; // 兜底
        }
        
        // 3. 向量化
        String textToEmbed = rawTag + " " + context;
        float[] vectorFloat = embeddingModel.embed(textToEmbed);
        List<Double> vector = new ArrayList<>();
        for (float v : vectorFloat) {
            vector.add((double) v);
        }

        // 4. 保存标签
        EvaluationTag tag = new EvaluationTag();
        tag.setCreatorEmployee(fromUser);
        tag.setTargetEmployee(targetUser);
        tag.setRawTagName(rawTag);
        tag.setContext(context);
        tag.setWeight(weight);
        tag.setStandardizedCategory(category);
        tag.setVector(vector);

        tagRepo.save(tag);

        // 5. 给评价者发低保奖励 (辛苦分)
        rewardService.addPoints(fromUser, 5, "提交评价: " + rawTag);
    }
}

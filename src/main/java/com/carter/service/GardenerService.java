package com.carter.service;

import com.carter.common.Constants;
import com.carter.common.VectorUtils;
import com.carter.entity.ContributorProfile;
import com.carter.entity.EvaluationTag;
import com.carter.entity.SkillRecord;
import com.carter.entity.enums.StandardCompetency;
import com.carter.exception.DendriteException;
import com.carter.exception.DendriteException.ErrorCode;
import com.carter.repo.ContributorProfileRepository;
import com.carter.repo.EvaluationTagRepository;
import com.carter.repo.SkillRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Core service for processing employee evaluations.
 * Extracts skills using AI and manages talent data.
 *
 * @author Carter
 * @since 1.0.0
 */
@Service
public class GardenerService {

    private static final Logger log = LoggerFactory.getLogger(GardenerService.class);

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

    // ==========================================
    // AI Response DTOs
    // ==========================================

    public record SkillExtractionResult(String skillName, String proficiency, String evidence) {}
    public record AiResponse(List<SkillExtractionResult> skills) {}
    public record BatchSkillResult(String employeeName, List<SkillExtractionResult> skills) {}
    public record BatchAiResponse(List<BatchSkillResult> results) {}

    // ==========================================
    // Evaluation Processing
    // ==========================================

    /**
     * Processes a single evaluation using AI to extract skills.
     *
     * @param targetEmployee the employee being evaluated
     * @param rawText the evaluation content
     * @return list of extracted skill records
     * @throws DendriteException if AI parsing fails
     */
    public List<SkillRecord> processEvaluation(String targetEmployee, String rawText) {
        log.info("Processing evaluation for employee: {}", targetEmployee);

        var converter = new BeanOutputConverter<>(AiResponse.class);
        String promptText = buildEvaluationPrompt(targetEmployee, rawText, converter.getFormat());

        String response = chatClient.prompt(promptText).call().content();
        AiResponse aiData = converter.convert(response);

        if (aiData == null || aiData.skills() == null) {
            log.warn("AI returned empty result for employee: {}", targetEmployee);
            return List.of();
        }

        List<SkillRecord> records = aiData.skills().stream()
                .map(skill -> createSkillRecord(targetEmployee, skill))
                .toList();

        log.info("Extracted {} skills for employee: {}", records.size(), targetEmployee);
        return skillRepository.saveAll(records);
    }

    /**
     * Processes evaluation for a single employee (used by pipeline).
     *
     * @param employeeName the employee name
     * @param mergedContent combined evaluation content
     * @return list of extracted skill records
     */
    public List<SkillRecord> processBatchEvaluation(String employeeName, String mergedContent) {
        return processEvaluation(employeeName, mergedContent);
    }

    /**
     * Processes evaluations for multiple employees in one AI call.
     *
     * @param employeeEvaluations map of employee names to evaluation content
     * @return all extracted skill records
     */
    public List<SkillRecord> processBatchEvaluations(Map<String, String> employeeEvaluations) {
        if (employeeEvaluations.isEmpty()) {
            return List.of();
        }

        log.info("Batch processing {} employee evaluations", employeeEvaluations.size());

        var converter = new BeanOutputConverter<>(BatchAiResponse.class);
        String promptText = buildBatchPrompt(employeeEvaluations, converter.getFormat());

        String response = chatClient.prompt(promptText).call().content();
        BatchAiResponse batchData = converter.convert(response);

        if (batchData == null || batchData.results() == null) {
            log.warn("AI returned empty batch result");
            return List.of();
        }

        List<SkillRecord> allRecords = new ArrayList<>();
        for (BatchSkillResult employeeResult : batchData.results()) {
            if (employeeResult.skills() == null) {
                continue;
            }

            List<SkillRecord> records = employeeResult.skills().stream()
                    .map(skill -> createSkillRecord(employeeResult.employeeName(), skill))
                    .toList();

            allRecords.addAll(skillRepository.saveAll(records));
        }

        log.info("Batch extracted {} total skills", allRecords.size());
        return allRecords;
    }

    // ==========================================
    // Tag Processing
    // ==========================================

    /**
     * Processes a user-submitted evaluation tag.
     * Classifies using AI and stores with contributor weight.
     *
     * @param fromUser the contributor submitting the tag
     * @param targetUser the employee being tagged
     * @param rawTag the tag content
     * @param context additional context
     */
    public void processUserTag(String fromUser, String targetUser, String rawTag, String context) {
        log.info("Processing tag from {} for {}: {}", fromUser, targetUser, rawTag);

        ContributorProfile contributor = getOrCreateContributor(fromUser);
        double weight = calculateWeight(contributor);

        StandardCompetency category = classifyTag(rawTag, context);
        List<Double> vector = generateVector(rawTag + " " + context);

        EvaluationTag tag = new EvaluationTag();
        tag.setCreatorEmployee(fromUser);
        tag.setTargetEmployee(targetUser);
        tag.setRawTagName(rawTag);
        tag.setContext(context);
        tag.setWeight(weight);
        tag.setStandardizedCategory(category);
        tag.setVector(vector);

        tagRepo.save(tag);
        rewardService.addPoints(fromUser, Constants.EVALUATION_SUBMIT_REWARD, "Submitted tag: " + rawTag);

        log.info("Tag saved with category: {}, weight: {}", category, weight);
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    private SkillRecord createSkillRecord(String employeeName, SkillExtractionResult skill) {
        SkillRecord record = new SkillRecord();
        record.setEmployeeName(employeeName);
        record.setSkillName(skill.skillName());
        record.setProficiency(skill.proficiency());
        record.setEvidence(skill.evidence());
        record.setEmbedding(generateVector(skill.skillName() + ": " + skill.evidence()));
        return record;
    }

    private List<Double> generateVector(String text) {
        float[] vectorFloat = embeddingModel.embed(text);
        return VectorUtils.toDoubleList(vectorFloat);
    }

    private ContributorProfile getOrCreateContributor(String employeeName) {
        return contributorRepo.findByEmployeeName(employeeName)
                .orElseGet(() -> {
                    rewardService.addPoints(employeeName, 0, "Account initialized");
                    return contributorRepo.findByEmployeeName(employeeName)
                            .orElseThrow(() -> new DendriteException(ErrorCode.EMPLOYEE_NOT_FOUND, employeeName));
                });
    }

    private double calculateWeight(ContributorProfile contributor) {
        return Constants.BASE_WEIGHT + (contributor.getLevel() - 1) * Constants.LEVEL_WEIGHT_INCREMENT;
    }

    private StandardCompetency classifyTag(String rawTag, String context) {
        String prompt = """
                Classify this evaluation tag into a standard competency:
                Tag: "%s"
                Context: "%s"

                Standard competency list:
                %s

                Return only the enum value, nothing else.
                """.formatted(rawTag, context, Arrays.toString(StandardCompetency.values()));

        String categoryStr = chatClient.prompt(prompt).call().content();

        try {
            String cleanCategory = categoryStr.replaceAll("[^a-zA-Z_]", "").toUpperCase();
            return StandardCompetency.valueOf(cleanCategory);
        } catch (Exception e) {
            log.warn("Failed to parse category '{}', using default", categoryStr);
            return StandardCompetency.HARD_SKILL_GENERAL;
        }
    }

    private String buildEvaluationPrompt(String employee, String content, String format) {
        return """
                你是一位专业的人才分析师。请分析以下员工评价：
                
                员工姓名：%s
                评价内容："%s"

                请提取评价中体现的技术技能和软技能。
                对于每个技能：
                1. skillName：技能名称（用中文，如"Java开发"、"沟通能力"）
                2. proficiency：熟练度（入门/熟练/精通/专家）
                3. evidence：从原文中引用证据（保持原文语言）

                %s
                """.formatted(employee, content, format);
    }

    private String buildBatchPrompt(Map<String, String> evaluations, String format) {
        StringBuilder context = new StringBuilder();
        for (var entry : evaluations.entrySet()) {
            context.append(String.format("[员工：%s]\n评价内容：%s\n\n",
                    entry.getKey(), entry.getValue()));
        }

        return """
                你是一位专业的人才分析师。请批量分析以下员工评价：

                %s

                对于每位员工：
                1. 提取技术技能和软技能（skillName用中文）
                2. 判断熟练度（入门/熟练/精通/专家）
                3. 从原文引用证据

                注意：employeeName必须与输入完全一致。

                %s
                """.formatted(context, format);
    }
}

package com.carter.service;

import com.carter.entity.SkillRecord;
import com.carter.entity.TalentProfile;
import com.carter.exception.DendriteException;
import com.carter.exception.DendriteException.ErrorCode;
import com.carter.repo.SkillRecordRepository;
import com.carter.repo.TalentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating AI-powered talent profile summaries.
 * Creates professional summaries and skill tags from evaluation data.
 *
 * @author Carter
 * @since 1.0.0
 */
@Service
public class SummarizerService {

    private static final Logger log = LoggerFactory.getLogger(SummarizerService.class);

    private static final String UPDATE_VECTOR_SQL =
            "UPDATE dendrite_profiles SET embedding = ?::vector WHERE id = ?";

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final SkillRecordRepository skillRepo;
    private final TalentProfileRepository profileRepo;
    private final JdbcTemplate jdbcTemplate;

    public SummarizerService(ChatClient.Builder builder,
                             EmbeddingModel embeddingModel,
                             SkillRecordRepository skillRepo,
                             TalentProfileRepository profileRepo,
                             JdbcTemplate jdbcTemplate) {
        this.chatClient = builder.build();
        this.embeddingModel = embeddingModel;
        this.skillRepo = skillRepo;
        this.profileRepo = profileRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * AI response DTO for bilingual profile summary.
     */
    public record ProfileSummary(
            String summaryZh,      // Chinese summary
            String summaryEn,      // English summary
            List<String> tagsZh,   // Chinese skill tags
            List<String> tagsEn    // English skill tags
    ) {}

    /**
     * Generates or updates a talent profile for an employee.
     * Creates AI-powered summary and vector embedding.
     *
     * @param employeeName the employee to summarize
     * @return the generated/updated profile
     * @throws DendriteException if no evaluation data exists
     */
    @Transactional
    public TalentProfile generateProfile(String employeeName) {
        log.info("Generating profile for employee: {}", employeeName);

        List<SkillRecord> records = skillRepo.findByEmployeeName(employeeName);
        if (records.isEmpty()) {
            throw new DendriteException(ErrorCode.EMPLOYEE_NO_DATA, employeeName);
        }

        ProfileSummary aiResult = generateAiSummary(employeeName, records);
        TalentProfile profile = saveProfile(employeeName, aiResult);
        updateProfileVector(profile, aiResult);

        log.info("Profile generated for {}: {} skills extracted", employeeName,
                aiResult != null && aiResult.tagsZh() != null ? aiResult.tagsZh().size() : 0);

        return profile;
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    private ProfileSummary generateAiSummary(String employeeName, List<SkillRecord> records) {
        String rawEvidence = records.stream()
                .map(r -> String.format("- %s (%s): %s",
                        r.getSkillName(), r.getProficiency(), r.getEvidence()))
                .collect(Collectors.joining("\n"));

        var converter = new BeanOutputConverter<>(ProfileSummary.class);

        String prompt = """
                你是一位专业的人才分析师。请根据以下评价信息生成【双语】人才画像：
                
                员工姓名："%s"
                评价记录：
                %s

                请同时生成中文和英文版本：
                
                1. summaryZh：中文职业简介（约200字），突出核心能力、工作风格和价值
                2. summaryEn：英文职业简介（约150 words），与中文内容对应
                3. tagsZh：5-10个中文技能标签（如：Java开发、问题解决、团队协作）
                4. tagsEn：对应的英文技能标签（如：Java Development, Problem Solving, Teamwork）
                
                注意：中英文标签数量必须一致，一一对应。
                
                %s
                """.formatted(employeeName, rawEvidence, converter.getFormat());

        String response = chatClient.prompt(prompt).call().content();
        return converter.convert(response);
    }

    private TalentProfile saveProfile(String employeeName, ProfileSummary aiResult) {
        TalentProfile profile = profileRepo.findByEmployeeName(employeeName)
                .orElseGet(TalentProfile::new);

        profile.setEmployeeName(employeeName);
        if (aiResult != null) {
            // Bilingual summaries
            profile.setSummaryZh(aiResult.summaryZh());
            profile.setSummaryEn(aiResult.summaryEn());
            // Bilingual skill tags
            profile.setSkillsZh(aiResult.tagsZh());
            profile.setSkillsEn(aiResult.tagsEn());
        }
        profile.setLastUpdated(LocalDateTime.now());

        return profileRepo.save(profile);
    }

    private void updateProfileVector(TalentProfile profile, ProfileSummary aiResult) {
        if (aiResult == null || aiResult.summaryEn() == null) {
            return;
        }

        // Use English summary for vector (better for semantic search)
        String textForVector = aiResult.summaryEn() + " " + String.join(", ", aiResult.tagsEn());
        float[] vector = embeddingModel.embed(textForVector);
        jdbcTemplate.update(UPDATE_VECTOR_SQL, vector, profile.getId());

        log.debug("Vector updated for profile: {}", profile.getId());
    }
}

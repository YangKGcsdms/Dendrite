package com.carter.service;

import com.carter.entity.SkillRecord;
import com.carter.entity.TalentProfile;
import com.carter.repo.SkillRecordRepository;
import com.carter.repo.TalentProfileRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.embedding.EmbeddingModel; // 👈 新引入
import org.springframework.jdbc.core.JdbcTemplate;     // 👈 新引入
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SummarizerService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel; // 向量模型
    private final SkillRecordRepository skillRepo;
    private final TalentProfileRepository profileRepo;
    private final JdbcTemplate jdbcTemplate;     // JDBC 工具

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

    public record ProfileSummary(String summary, List<String> tags) {}

    @Transactional // 开启事务，保证数据一致性
    public TalentProfile generateProfile(String employeeName) {
        // 1. 捞取数据
        List<SkillRecord> records = skillRepo.findByEmployeeName(employeeName);
        if (records.isEmpty()) {
            throw new RuntimeException("员工 " + employeeName + " 暂无数据");
        }

        // 2. 准备 Prompt
        String rawEvidence = records.stream()
                .map(r -> String.format("- %s (%s): %s", r.getSkillName(), r.getProficiency(), r.getEvidence()))
                .collect(Collectors.joining("\n"));

        var converter = new BeanOutputConverter<>(ProfileSummary.class);

        String prompt = """
                基于以下评价生成人才画像：
                员工："%s"
                评价集：
                %s
                
                要求：
                1. 生成一段 200 字的职业总结。
                2. 提炼 5-10 个技能标签。
                %s
                """.formatted(employeeName, rawEvidence, converter.getFormat());

        // 3. AI 生成文本 (Chat)
        String response = chatClient.prompt(prompt).call().content();
        ProfileSummary aiResult = converter.convert(response);

        // 4. 保存普通数据 (JPA)
        TalentProfile profile = profileRepo.findByEmployeeName(employeeName)
                .orElse(new TalentProfile());
        profile.setEmployeeName(employeeName);
        if (aiResult != null) {
            profile.setProfessionalSummary(aiResult.summary());
            profile.setTopSkills(aiResult.tags());
        }
        profile.setLastUpdated(java.time.LocalDateTime.now());

        TalentProfile savedProfile = profileRepo.save(profile); // 先保存，拿到 ID

        // ==========================================
        // 5. 注入灵魂：生成向量并更新 (JDBC)
        // ==========================================
        if (aiResult != null && aiResult.summary() != null) {
            // A. 把“职业总结”变成向量 (耗时约 100-300ms)
            float[] vector = embeddingModel.embed(aiResult.summary());

            // B. 手动写 SQL 更新向量字段 (绕过 Hibernate)
            // 注意：PGvector 这里的语法是 ?::vector
            String sql = "UPDATE dendrite_profiles SET embedding = ?::vector WHERE id = ?";

            // 需要把 float[] 转换成 Postgres 认识的格式，Spring AI 的 EmbeddingModel 通常返回 float[]
            // JdbcTemplate 可以直接处理数组，或者我们需要转成 String (如 "[0.1, 0.2...]")
            // 简单做法：直接传 float[] 数组，pgjdbc 驱动通常能处理
            jdbcTemplate.update(sql, vector, savedProfile.getId());
        }

        return savedProfile;
    }
}

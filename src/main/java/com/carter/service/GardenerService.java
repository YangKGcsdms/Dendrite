package com.carter.service;

import com.carter.entity.SkillRecord;
import com.carter.repo.SkillRecordRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

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

    public GardenerService(ChatClient.Builder builder, SkillRecordRepository skillRepository) {
        this.chatClient = builder.build();
        this.skillRepository = skillRepository;
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
                return record;
            }).toList();

            return skillRepository.saveAll(records);
        }
        return List.of();
    }
}

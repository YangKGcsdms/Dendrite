package com.carter.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "dendrite_profiles")
public class TalentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 唯一索引，每个员工只有一张画像
    @Column(unique = true)
    private String employeeName;

    // AI 生成的 200 字职业总结
    @Column(length = 2000)
    private String professionalSummary;

    // AI 归纳后的去重技能标签 (存成 JSON 字符串或简单列表)
    // 比如: ["Redis", "Spring Boot", "沟通能力"]
    @ElementCollection
    private List<String> topSkills;

    // 最后更新时间
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // 它是所有 SkillRecord 向量的平均值，或者是 AI 专门生成的一段 Summary 的向量
    @Convert(converter = com.carter.converter.VectorToStringConverter.class)
    @Column(columnDefinition = "vector(768)")
    @org.hibernate.annotations.ColumnTransformer(write = "?::vector", read = "embedding::text")
    private List<Double> embedding;
}

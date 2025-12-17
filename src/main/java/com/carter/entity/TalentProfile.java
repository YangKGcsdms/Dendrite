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

    // (预留) 向量字段，未来搜人就搜这个字段
    // private List<Double> embedding;
}

package com.carter.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Carter
 * @date 2025/12/16
 * @description
 */
@Entity
@Data
@Table(name = "dendrite_skills")
public class SkillRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 被评价人 (比如: "Carter")
    private String employeeName;

    // 技能名称 (AI 提取，比如: "Spring Boot")
    private String skillName;

    // 熟练度 (AI 判断: 初级/中级/精通)
    private String proficiency;

    // 原始证据 (原文片段)
    @Column(length = 1000)
    private String evidence;

    // 创建时间
    private LocalDateTime createdAt = LocalDateTime.now();
}

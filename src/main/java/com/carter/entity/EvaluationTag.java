package com.carter.entity;


import com.carter.entity.enums.StandardCompetency;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Carter
 * @date 2025/12/17
 * @description
 */
@Entity
@Data
@Table(name = "dendrite_evaluation_tags")
public class EvaluationTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === 关系链 ===
    private String targetEmployee; // 被评价人 (如: Leo)

    // 评价人 (如: Alice) -> 关联到 ContributorProfile
    private String creatorEmployee;

    // === 标签内容 ===
    // 原始标签 (如: "#Redis救火队员")
    private String rawTagName;

    // 佐证/语境 (如: "昨晚帮我排查了连接池泄露")
    @Column(length = 1000)
    private String context;

    // AI 映射后的标准分类
    @Enumerated(EnumType.STRING)
    private StandardCompetency standardizedCategory;

    // === 向量化数据 ===
    // 存储: rawTagName + standardizedCategory + context 的混合向量
    @Convert(converter = com.carter.converter.VectorToStringConverter.class)
    @Column(columnDefinition = "vector(768)")
    @org.hibernate.annotations.ColumnTransformer(write = "?::vector", read = "vector::text")
    private List<Double> vector;

    // === 进化参数 ===
    // 初始权重 = 评价者等级系数 * AI置信度
    // 随着点赞/搜索命中，这个权重会动态增加！
    private Double weight = 1.0;

    private LocalDateTime createdAt = LocalDateTime.now();
}
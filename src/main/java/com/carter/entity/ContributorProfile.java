package com.carter.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "dendrite_contributors")
public class ContributorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联的员工账号 (唯一)
    @Column(unique = true, nullable = false)
    private String employeeName;

    // === 核心资产 (Gamification) ===

    // 当前积分 (可消费)
    private Long currentPoints = 0L;

    // 历史总积分 (只增不减，用于计算等级)
    private Long totalAccumulatedPoints = 0L;

    // 伯乐等级 (Lv1 - Lv5)
    // 等级越高，打出的标签初始权重(Weight)越高
    private Integer level = 1;

    // === 鉴赏力画像 (Meta-Embedding) ===
    // 这是一个很有趣的向量：记录该用户"喜欢评价什么样的人"
    // 系统可以用它来给伯乐推荐"你可能感兴趣的待评价同事"
    @Convert(converter = com.carter.converter.VectorToStringConverter.class)
    @Column(columnDefinition = "vector(768)")
    @org.hibernate.annotations.ColumnTransformer(write = "?::vector", read = "taste_embedding::text")
    private List<Double> tasteEmbedding;

    // === 统计指标 ===
    private Integer totalTagsSubmitted = 0;  // 提交总数
    private Integer searchHitsCount = 0;     // 助攻次数 (最高荣誉)

    // 乐观锁 (防止并发修改积分)
    @Version
    private Long version;
}

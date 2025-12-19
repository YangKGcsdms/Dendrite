package com.carter.entity;


import com.carter.entity.enums.InteractionType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Carter
 * @date 2025/12/17
 * @description
 */
@Entity
@Data
@Table(name = "dendrite_tag_interactions")
public class TagInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 哪个标签产生了价值？
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private EvaluationTag evaluationTag;

    // 交互类型 (SEARCH_HIT, UPVOTE...)
    @Enumerated(EnumType.STRING)
    private InteractionType type;

    // 触发者 (是谁搜到了这个标签？是谁点的赞？)
    private String triggerUser;

    // 关联的搜索词 (如果是 SEARCH_HIT，记录用户当时搜了什么)
    private String relatedQuery;

    private LocalDateTime timestamp = LocalDateTime.now();
}

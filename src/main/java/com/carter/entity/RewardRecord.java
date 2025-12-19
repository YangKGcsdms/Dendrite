package com.carter.entity;


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
@Table(name = "dendrite_reward_records")
public class RewardRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 受益人 (伯乐)
    private String employeeName;

    // 变动分数 (+10, +100)
    private Integer pointsChange;

    // 变动原因 (如: "搜索助攻", "获得点赞")
    private String reason;

    // 关联的交互事件 ID (可选，用于溯源)
    private Long sourceInteractionId;

    private LocalDateTime timestamp = LocalDateTime.now();
}

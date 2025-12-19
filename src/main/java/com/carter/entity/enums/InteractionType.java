package com.carter.entity.enums;


/**
 * @author Carter
 * @date 2025/12/17
 * @description
 */
public enum InteractionType {
    // 强信号 (高奖励)
    SEARCH_HIT,     // 关键：该标签帮助用户在搜索中找到了人 (召回贡献)

    // 中信号 (中奖励)
    UPVOTE,         // 被其他人点赞/附议
    AI_VALIDATED,   // 通过了 AI 的逻辑一致性检查

    // 弱信号 (低奖励)
    VIEWED,         // 被查看

    // 负信号 (惩罚)
    DOWNVOTE,       // 被踩/举报
    REJECTED        // 被 AI 判定为垃圾信息
}

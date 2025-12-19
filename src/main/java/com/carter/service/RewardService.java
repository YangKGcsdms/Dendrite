package com.carter.service;


import com.carter.entity.ContributorProfile;
import com.carter.entity.RewardRecord;
import com.carter.repo.ContributorProfileRepository;
import com.carter.repo.RewardRecordRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * @author Carter
 * @date 2025/12/17
 * @description
 */
@Service
public class RewardService {

    private final ContributorProfileRepository contributorRepo;
    private final RewardRecordRepository rewardRepo;

    public RewardService(ContributorProfileRepository contributorRepo, RewardRecordRepository rewardRepo) {
        this.contributorRepo = contributorRepo;
        this.rewardRepo = rewardRepo;
    }

    /**
     * 核心方法：给员工加分
     * @param employeeName 员工姓名
     * @param points 分数 (可以是负数)
     * @param reason 原因
     */
    @Transactional
    public void addPoints(String employeeName, int points, String reason) {
        // 1. 找到或新建伯乐档案
        ContributorProfile profile = contributorRepo.findByEmployeeName(employeeName)
                .orElseGet(() -> createNewContributor(employeeName));

        // 2. 更新分数
        profile.setCurrentPoints(profile.getCurrentPoints() + points);
        if (points > 0) {
            profile.setTotalAccumulatedPoints(profile.getTotalAccumulatedPoints() + points);
        }

        // 3. 检查升级 (每 100 分升 1 级，最高 5 级)
        int newLevel = (int) (profile.getTotalAccumulatedPoints() / 100) + 1;
        if (newLevel > 5) newLevel = 5;
        if (newLevel > profile.getLevel()) {
            // 这里可以发一个"升级通知"事件
            profile.setLevel(newLevel);
            // 额外奖励升级积分
            reason += " (等级提升至 Lv" + newLevel + ")";
        }

        contributorRepo.save(profile);

        // 4. 记流水
        RewardRecord record = new RewardRecord();
        record.setEmployeeName(employeeName);
        record.setPointsChange(points);
        record.setReason(reason);
        rewardRepo.save(record);
    }

    /**
     * 辅助方法：初始化新用户
     */
    private ContributorProfile createNewContributor(String name) {
        ContributorProfile p = new ContributorProfile();
        p.setEmployeeName(name);
        p.setLevel(1);
        p.setCurrentPoints(0L);
        p.setTotalAccumulatedPoints(0L);
        return contributorRepo.save(p); // 先保存以获取ID
    }

}

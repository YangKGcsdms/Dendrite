package com.carter.repo;


import com.carter.entity.RewardRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Carter
 * @date 2025/12/17
 * @description
 */
public interface RewardRecordRepository extends JpaRepository<RewardRecord, Long> {
    // 查某个人的积分流水
    List<RewardRecord> findByEmployeeNameOrderByTimestampDesc(String employeeName);
}

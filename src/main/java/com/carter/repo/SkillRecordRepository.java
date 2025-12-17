package com.carter.repo;

import com.carter.entity.SkillRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Carter
 * @date 2025/12/16
 * @description
 */
public interface SkillRecordRepository extends JpaRepository<SkillRecord, Long> {

    List<SkillRecord> findByEmployeeName(String employeeName);

}

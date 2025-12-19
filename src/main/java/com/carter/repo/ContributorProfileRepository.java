package com.carter.repo;


import com.carter.entity.ContributorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Carter
 * @date 2025/12/17
 * @description
 */
public interface ContributorProfileRepository extends JpaRepository<ContributorProfile, Long> {
    // 根据员工名字查找伯乐档案
    Optional<ContributorProfile> findByEmployeeName(String employeeName);
}

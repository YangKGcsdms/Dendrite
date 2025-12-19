package com.carter.repo;


import com.carter.entity.EvaluationTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Carter
 * @date 2025/12/17
 * @description
 */
public interface EvaluationTagRepository extends JpaRepository<EvaluationTag, Long> {
    // 查某个员工被贴了哪些标签
    List<EvaluationTag> findByTargetEmployee(String targetEmployee);

    // 查某个人贴过哪些标签（用于分析他的贡献）
    List<EvaluationTag> findByCreatorEmployee(String creatorEmployee);
}
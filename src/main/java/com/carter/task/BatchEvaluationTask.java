package com.carter.task;

import java.io.Serializable;
import java.util.List;

/**
 * 批量评价任务包装类
 * 用于将多个评价任务组合在一起批量处理
 */
public record BatchEvaluationTask(List<EvaluationTask> tasks) implements Serializable {

    /**
     * 获取本批次涉及的所有员工名单 (去重)
     */
    public List<String> getDistinctEmployees() {
        return tasks.stream()
                .map(EvaluationTask::employeeName)
                .distinct()
                .toList();
    }

    /**
     * 获取某个员工的所有评价内容合并
     */
    public String getMergedContentFor(String employeeName) {
        return tasks.stream()
                .filter(t -> t.employeeName().equals(employeeName))
                .map(EvaluationTask::rawContent)
                .reduce((a, b) -> a + "\n---\n" + b)
                .orElse("");
    }
}


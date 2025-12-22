package com.carter.task.worker;

import com.carter.pipeline.EvaluationPipeline;
import com.carter.task.BatchEvaluationTask;
import com.carter.task.EvaluationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 评价队列消费者 Worker
 * 
 * 改造后的特性：
 * 1. 扫描频率：5分钟一次 (300000ms)
 * 2. 批量处理：每次最多取10条
 * 3. Pipeline：评价 -> 总结 -> 向量存储
 */
@Component
public class GardenerWorker {

    private static final Logger log = LoggerFactory.getLogger(GardenerWorker.class);
    private static final String QUEUE_KEY = "dendrite:evaluation:queue";
    
    // 配置常量
    private static final int MAX_BATCH_SIZE = 10;  // 每次最多处理10条

    private final RedisTemplate<String, Object> redisTemplate;
    private final EvaluationPipeline evaluationPipeline;

    public GardenerWorker(RedisTemplate<String, Object> redisTemplate,
                          EvaluationPipeline evaluationPipeline) {
        this.redisTemplate = redisTemplate;
        this.evaluationPipeline = evaluationPipeline;
    }

    /**
     * 定时任务：每5分钟扫描一次队列
     * cron表达式：0 0/5 * * * ? 表示每5分钟执行一次
     * 
     * 也可以使用 fixedRate = 300000 (5分钟 = 5 * 60 * 1000 ms)
     */
    @Scheduled(fixedRate = 300000, initialDelay = 10000) // 5分钟扫描一次，启动后延迟10秒开始
    public void processQueueBatch() {
        log.info("⏰ [Worker] 定时扫描启动，开始检查评价队列...");
        
        try {
            // 1. 批量取出任务 (最多10条)
            List<EvaluationTask> tasks = fetchBatchTasks();
            
            if (tasks.isEmpty()) {
                log.info("📭 [Worker] 队列为空，本次扫描结束");
                return;
            }

            log.info("📦 [Worker] 获取到 {} 条待处理任务", tasks.size());

            // 2. 打包成批量任务
            BatchEvaluationTask batchTask = new BatchEvaluationTask(tasks);

            // 3. 执行 Pipeline (评价 -> 总结 -> 向量存储)
            EvaluationPipeline.PipelineResult result = evaluationPipeline.execute(batchTask);

            // 4. 输出结果
            if (result.isSuccess()) {
                log.info("🎉 [Worker] 批量处理完成! 技能记录: {}, 画像更新: {}, 向量存储: {}, 耗时: {}ms",
                        result.getEvaluatedCount(),
                        result.getProfilesUpdated(),
                        result.getVectorsStored(),
                        result.getDurationMs());
            } else {
                log.error("❌ [Worker] 批量处理失败: {}", result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("❌ [Worker] 处理队列时发生异常", e);
        }
    }

    /**
     * 从 Redis 批量获取任务
     * @return 任务列表 (最多 MAX_BATCH_SIZE 条)
     */
    private List<EvaluationTask> fetchBatchTasks() {
        List<EvaluationTask> tasks = new ArrayList<>();

        for (int i = 0; i < MAX_BATCH_SIZE; i++) {
            // 从队列右侧弹出 (FIFO)
            Object rawTask = redisTemplate.opsForList().rightPop(QUEUE_KEY);
            
            if (rawTask == null) {
                // 队列空了，返回已获取的任务
                break;
            }

            if (rawTask instanceof EvaluationTask task) {
                tasks.add(task);
            } else {
                log.warn("⚠️ [Worker] 无法识别的任务类型: {}", rawTask.getClass().getName());
            }
        }

        return tasks;
    }

    /**
     * 手动触发处理 (供测试或紧急情况使用)
     * 可以通过 Actuator 或管理接口调用
     */
    public EvaluationPipeline.PipelineResult triggerManualProcess() {
        log.info("🔧 [Worker] 手动触发处理...");
        
        List<EvaluationTask> tasks = fetchBatchTasks();
        if (tasks.isEmpty()) {
            log.info("📭 [Worker] 队列为空");
            return null;
        }

        return evaluationPipeline.execute(new BatchEvaluationTask(tasks));
    }

    /**
     * 获取当前队列长度
     */
    public Long getQueueSize() {
        return redisTemplate.opsForList().size(QUEUE_KEY);
    }
}

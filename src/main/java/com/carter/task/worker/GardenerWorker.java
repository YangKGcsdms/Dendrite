package com.carter.task.worker;

import com.carter.service.GardenerService;
import com.carter.task.EvaluationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GardenerWorker {

    private static final Logger log = LoggerFactory.getLogger(GardenerWorker.class);
    private static final String QUEUE_KEY = "dendrite:evaluation:queue";

    private final RedisTemplate<String, Object> redisTemplate;
    private final GardenerService gardenerService;

    public GardenerWorker(RedisTemplate<String, Object> redisTemplate, GardenerService gardenerService) {
        this.redisTemplate = redisTemplate;
        this.gardenerService = gardenerService;
    }

    /**
     * 轮询机制：每隔 100ms 检查一次队列
     * 只要队列里有货，就拿出来处理
     */
    @Scheduled(fixedDelay = 100)
    public void processQueue() {
        try {
            // 1. 从 Redis 队列右侧弹出一个任务 (右出 Right Pop -> 先进先出 FIFO)
            Object rawTask = redisTemplate.opsForList().rightPop(QUEUE_KEY);

            // 2. 如果没拿到任务，直接结束本次轮询
            if (rawTask == null) {
                return;
            }

            // 3. 转换并处理
            if (rawTask instanceof EvaluationTask task) {
                log.info("⚙️ [Worker] 收到任务: 正在分析员工 {} 的评价...", task.employeeName());

                // 调用核心 AI 服务 (这是最耗时的一步)
                gardenerService.processEvaluation(task.employeeName(), task.rawContent());

                log.info("✅ [Worker] 分析完成: {} 的数据已入库。", task.employeeName());
            }

        } catch (Exception e) {
            log.error("❌ [Worker] 处理任务时发生异常", e);
            // 生产环境建议：这里应该把失败的任务塞回死信队列，防止丢消息
        }
    }
}
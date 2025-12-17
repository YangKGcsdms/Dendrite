package com.carter.controller;

import com.carter.entity.TalentProfile; // 👈 确保引入了你刚才建的 Entity
import com.carter.service.SearchService;
import com.carter.service.SummarizerService;
import com.carter.task.EvaluationTask;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gardener")
public class GardenerController {

    private final SearchService searchService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SummarizerService summarizerService; // ✅ 1. 声明服务

    private static final String QUEUE_KEY = "dendrite:evaluation:queue";

    // ✅ 2. 关键修改：构造函数必须包含 SummarizerService，Spring 才能注入进来
    public GardenerController(RedisTemplate<String, Object> redisTemplate,
                              SummarizerService summarizerService,
                              SearchService searchService) {
        this.redisTemplate = redisTemplate;
        this.summarizerService = summarizerService;
        this.searchService = searchService;
    }

    /**
     * 阶段一：异步接收评价 (原始层 Raw Layer)
     */
    @PostMapping("/evaluate")
    public Map<String, Object> submitEvaluation(@RequestParam String employee, @RequestBody String content) {
        // 1. 打包任务
        EvaluationTask task = new EvaluationTask(employee, content);

        // 2. 扔进 Redis
        redisTemplate.opsForList().leftPush(QUEUE_KEY, task);

        // 3. 返回
        return Map.of(
                "success", true,
                "message", "评价已提交至处理队列，园丁AI稍后分析。",
                "employee", employee,
                "status", "queued"
        );
    }

    /**
     * ✅ 3. 新增阶段二：触发 AI 自总结 (画像层 Profile Layer)
     * URL: POST http://localhost:8080/api/gardener/summarize?employee=Carter
     */
    @PostMapping("/summarize")
    public TalentProfile summarizeEmployee(@RequestParam String employee) {
        // 直接调用总结服务，生成或更新画像
        return summarizerService.generateProfile(employee);
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String query) {
        return searchService.searchSimilarProfiles(query, 5); // 默认搜前 5 名
    }
}
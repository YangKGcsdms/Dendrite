package com.carter.controller;


import com.carter.entity.SkillRecord;
import com.carter.service.GardenerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gardener")
public class GardenerController {

    private final GardenerService gardenerService;

    public GardenerController(GardenerService gardenerService) {
        this.gardenerService = gardenerService;
    }

    /**
     * 发送评价，触发 AI 分析
     * POST http://localhost:8080/api/gardener/evaluate?employee=Carter
     * Body (Raw Text): Carter 最近在负责重构旧系统，他熟练使用了 Spring Boot 3.0 的新特性，
     * 把启动速度提升了50%。但是在编写文档方面有点偷懒。
     */
    @PostMapping("/evaluate")
    public List<SkillRecord> evaluate(@RequestParam String employee, @RequestBody String content) {
        return gardenerService.processEvaluation(employee, content);
    }
}

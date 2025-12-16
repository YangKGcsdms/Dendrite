package com.carter.dendrite;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloAIController {

    private final ChatClient chatClient;

    // 构造器注入 ChatClient.Builder，Spring AI 自动提供
    public HelloAIController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 测试接口：让 AI 用一种风格自我介绍
     * 访问地址: http://localhost:8080/hello?style=赛博朋克
     */
    @GetMapping("/hello")
    public Map<String, String> sayHello(@RequestParam(defaultValue = "幽默") String style) {

        String prompt = """
                你现在是 "Dendrite (树突)" 系统的智能核心。
                请用【%s】的风格，向你的创造者 Carter 问好。
                简短一点，50字以内。
                """.formatted(style);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return Map.of(
                "style", style,
                "response", response
        );
    }
}
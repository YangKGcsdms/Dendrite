package com.carter.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author Carter
 * @date 2025/12/16
 * @description
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 使用 String 序列化 (方便你在 Redis Desktop Manager 里看)
        template.setKeySerializer(new StringRedisSerializer());

        // Value 使用 JSON 序列化 (对象自动转 JSON 存进去)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
package com.carter.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for high-performance search.
 * Uses in-memory caching to reduce database and AI calls.
 *
 * @author Carter
 * @since 1.0.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Simple in-memory cache manager.
     * For production with multiple instances, consider Redis-based caching.
     *
     * Cache names:
     * - searchResults: caches vector search results
     * - profiles: caches talent profiles
     * - queryExpansion: caches AI query expansions
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "searchResults",
                "profiles",
                "queryExpansion"
        );
    }
}


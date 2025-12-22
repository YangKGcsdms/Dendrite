package com.carter.config;

import com.carter.dto.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting configuration to protect AI endpoints.
 * Prevents abuse and controls cost by limiting request rates.
 *
 * @author Carter
 * @since 1.0.0
 */
@Configuration
public class RateLimitConfig {

    /**
     * Rate limit filter for AI-intensive endpoints.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter());
        registration.addUrlPatterns("/api/v1/gardener/ask", "/api/v1/gardener/ask/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * Simple sliding window rate limiter.
     * In production, consider using Redis-based distributed rate limiting.
     */
    public static class RateLimitFilter implements Filter {

        // IP -> (timestamp -> count)
        private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

        // Limits: 30 AI requests per minute per IP
        private static final int MAX_REQUESTS_PER_MINUTE = 30;
        private static final long WINDOW_MS = 60_000;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String clientIp = getClientIp(httpRequest);

            if (isRateLimited(clientIp)) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");

                ApiResponse<Object> errorResponse = ApiResponse.error(
                        "Rate limit exceeded. Max " + MAX_REQUESTS_PER_MINUTE + " AI requests per minute."
                );
                httpResponse.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }

            chain.doFilter(request, response);
        }

        private boolean isRateLimited(String clientIp) {
            long now = System.currentTimeMillis();

            RequestCounter counter = requestCounts.compute(clientIp, (key, existing) -> {
                if (existing == null || now - existing.windowStart > WINDOW_MS) {
                    return new RequestCounter(now);
                }
                return existing;
            });

            return counter.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE;
        }

        private String getClientIp(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }

        private static class RequestCounter {
            final long windowStart;
            final AtomicInteger count;

            RequestCounter(long windowStart) {
                this.windowStart = windowStart;
                this.count = new AtomicInteger(0);
            }
        }
    }
}


package com.gene.sphere.geneservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Redis cache that verifies connectivity and read/write operations.
 * Exposed via Spring Boot Actuator at {@code /actuator/health}.
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Checks Redis health by pinging connection and testing read/write operations.
     *
     * @return Health status with details (UP if Redis is available, DOWN otherwise)
     */
    @Override
    public Health health() {
        try {
            String result = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equals(result)) {
                // Test the actual, read/write operations
                // Prevent collisions in concurrent health checks
                String testKey = "health:check: " + System.currentTimeMillis();
                redisTemplate.opsForValue().set(testKey, "test", java.time.Duration.ofSeconds(5));
                String testValue = (String) redisTemplate.opsForValue().get(testKey);
                redisTemplate.delete(testKey);

                return Health.up()
                        .withDetail("redis", "Available")
                        .withDetail("ping", result)
                        .withDetail("read_write", "test".equals(testValue) ? "OK" : "FAILED")
                        .withDetail("host", "localhost") // Will be dynamic in production
                        .withDetail("port", 6379)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Not Available")
                    .withDetail("error", e.getMessage())
                    .withDetail("error_type", e.getClass().getSimpleName())
                    .build();
        }
        return Health.down()
                .withDetail("redis", "Unknown status")
                .build();
    }
}

package com.gene.sphere.geneservice.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Production-ready Redis configuration with connection pooling and timeouts.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    // Empty password for localhost for now
    // TODO: change this to a real password when production ready
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration timeout;

    /**
     * Creates production-ready Redis connection factory with connection pooling.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        GenericObjectPoolConfig<?> poolConfig = getGenericObjectPoolConfig();

        // Client configuration with timeouts
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
        // If Redis command takes more than 2 seconds, give up
                .commandTimeout(Duration.ofSeconds(2))   
                // When the app shuts down, wait max 100ms for Redis connections to close cleanly   
                .shutdownTimeout(Duration.ofMillis(100))  
                // When app shuts down, wait max 100ms for Redis connections to close cleanly (gracefully shut down)  
                .poolConfig(poolConfig)      
                // Apply all the pool settings we configured above               
                .build();

        // Redis server configuration
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);

        // Set password if provided, empty for localhost for now
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    private static GenericObjectPoolConfig<?> getGenericObjectPoolConfig() {
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        // If all 20 connections are busy, the new requests must wait.
        poolConfig.setMaxTotal(20);
        // Max 10 connections that are ready to go instantly
        poolConfig.setMaxIdle(10);
        // Maintain at least 5 available connections ready for immediate use
        poolConfig.setMinIdle(5);
        // If all the 20 connections are busy, user needs to wait for 3 seconds maximum. After that, throw an error instead of waiting forever
        poolConfig.setMaxWaitMillis(3000);
        // Test the connection before giving it to your application
        poolConfig.setTestOnBorrow(true);
        // Test connection when app returns it to the pool
        poolConfig.setTestOnReturn(true);
        // Test the idle connections periodically
        poolConfig.setTestWhileIdle(true);

        // Run cleanup process every 60 seconds
        poolConfig.setTimeBetweenEvictionRunsMillis(60000);
        // During each clean up, test 3 connections
        poolConfig.setNumTestsPerEvictionRun(3);
        // When all the connections are busy, make the user wait instead of failing
        poolConfig.setBlockWhenExhausted(true);
        // Remove the connections that have been idle for more than 5 minutes
        poolConfig.setMinEvictableIdleTimeMillis(300000);
        // Remove 3+ minute idle connections only when pool is full
        poolConfig.setSoftMinEvictableIdleTimeMillis(180000);
        return poolConfig;
    }

    /**
     * Configures RedisTemplate with proper serializers for production use.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // The RedisTemplate bean, the main tool the app uses to talk to Redis
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // Create a new Redis template
        template.setConnectionFactory(connectionFactory);

        // Use String serializer to store keys (keys like: "gene:BRCA1" are stored exactly as readable text in Redis
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JSON serializer to store values (handles complex objects like GeneRecord)
        /*
        Key: "gene:BRCA1"
        Value: {
            "symbol": "BRCA1",
            "name": "BRCA1 DNA repair associated",
            "chromosome": "17",
            "mutations": [...],
            "timestamp": "2024-10-15T10:30:00Z"
        }*/
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Enable default serialization for all operations
        template.setEnableDefaultSerializer(true);
        template.setDefaultSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
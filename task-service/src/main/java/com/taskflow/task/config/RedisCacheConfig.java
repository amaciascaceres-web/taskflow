package com.taskflow.task.config;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskflow.task.controller.dto.TaskResponse;

@Configuration
public class RedisCacheConfig {

    @Value("${spring.cache.redis.time-to-live}")
    private long ttlMillis;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf, ObjectMapper springObjectMapper) {
        ObjectMapper mapper = springObjectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JavaType listOfTasks = mapper.getTypeFactory()
                .constructCollectionType(List.class, TaskResponse.class);

        return RedisCacheManager.builder(cf)
                .withCacheConfiguration("tasks", configFor(mapper, TaskResponse.class))
                .withCacheConfiguration("tasks-by-status", configFor(mapper, listOfTasks))
                .build();
    }

    private RedisCacheConfiguration configFor(ObjectMapper mapper, Class<?> type) {
        return configFor(mapper, mapper.getTypeFactory().constructType(type));
    }

    private RedisCacheConfiguration configFor(ObjectMapper mapper, JavaType type) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(ttlMillis))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new Jackson2JsonRedisSerializer<>(mapper, type)));
    }
}

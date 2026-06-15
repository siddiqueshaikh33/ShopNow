package com.example.demo;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;


@Configuration
@EnableCaching
public class RedisConfig {

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
	    ObjectMapper objectMapper = new ObjectMapper();
	    objectMapper.registerModule(new JavaTimeModule());
	    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	    objectMapper.activateDefaultTyping(
	        BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
	        ObjectMapper.DefaultTyping.NON_FINAL
	    );

	    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

	    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
	        .entryTtl(Duration.ofMinutes(10))
	        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

	    return RedisCacheManager.builder(factory)
	        .cacheDefaults(config)
	        .withCacheConfiguration("productsByCategory2", config)
	        .build();
	}

}

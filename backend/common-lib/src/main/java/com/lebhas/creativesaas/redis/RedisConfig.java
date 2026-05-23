package com.lebhas.creativesaas.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {

    @Bean(name = "redisObjectMapper")
    ObjectMapper redisObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    RedisScript<Long> compareAndDeleteRedisScript() {
        return RedisScript.of(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
                Long.class);
    }
}

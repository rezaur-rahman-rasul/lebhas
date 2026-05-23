package com.lebhas.creativesaas.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestJacksonConfiguration.class)
            .withUserConfiguration(RedisConfig.class);

    @Test
    void redisObjectMapperBeanDoesNotCreateCircularDependency() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RedisConfig.class);
            assertThat(context).hasBean("redisObjectMapper");

            ObjectMapper defaultMapper = context.getBean("defaultObjectMapper", ObjectMapper.class);
            ObjectMapper redisMapper = context.getBean("redisObjectMapper", ObjectMapper.class);

            assertThat(redisMapper).isNotNull();
            assertThat(redisMapper).isNotSameAs(defaultMapper);
        });
    }

    @Configuration
    static class TestJacksonConfiguration {

        @Bean
        ObjectMapper defaultObjectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}

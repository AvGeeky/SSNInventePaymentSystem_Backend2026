package com.saipbuilds.inventepayment2026.DBConfig.Redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.timeout:10000}")
    private long timeout;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);
        redisStandaloneConfiguration.setPassword(RedisPassword.of(password));

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .poolConfig(buildPoolConfig())
                .build();

        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    }

    private org.apache.commons.pool2.impl.GenericObjectPoolConfig buildPoolConfig() {
        org.apache.commons.pool2.impl.GenericObjectPoolConfig poolConfig = new org.apache.commons.pool2.impl.GenericObjectPoolConfig();
        // CHANGE 1: Set to 64 (slightly more than your max thread count of 60)
        poolConfig.setMaxTotal(64);

        // CHANGE 2: Increase Max Idle to keep connections ready
        poolConfig.setMaxIdle(32);

        // CHANGE 3: Keep a baseline of connections open so we don't constantly recreate them
        poolConfig.setMinIdle(16);

        // OPTIONAL: Fail fast if pool is exhausted (waits 2s by default, can be lowered)
        poolConfig.setMaxWaitMillis(2000);

        return poolConfig;
    }
}
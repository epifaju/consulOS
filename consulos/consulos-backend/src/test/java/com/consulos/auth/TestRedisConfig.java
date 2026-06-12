package com.consulos.auth;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class TestRedisConfig {

    private RedisServer redisServer;

    @Bean
    RedisServer embeddedRedisServer() throws IOException {
        redisServer = RedisServer.newRedisServer()
                .port(6370)
                .setting("requirepass redis_dev_pass")
                .build();
        redisServer.start();
        return redisServer;
    }

    @PreDestroy
    void stopRedis() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}

package com.saipbuilds.inventepayment2026.DBConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class Databasetest implements CommandLineRunner {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private static final Logger log = LoggerFactory.getLogger(Databasetest.class);

    public Databasetest(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }


    @Override
    public void run(String... args) throws Exception {
        //System.out.println("Testing database connection...");
//        System.out.println(System.getenv("SUPABASE_DB_URL"));
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                log.info("Successfully connected to the database!");
            } else {
                log.info("❌ Connection failed.");
            }
        } catch (Exception e) {
            log.warn ("❌ Error connecting to database:");
            log.warn("Exception: ", e);
        }
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            String pong = redis.ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                log.info("Redis is up and responding to PING.");
            } else {
                log.info("❌ Redis responded unexpectedly: {}", pong);
            }
        } catch (Exception e) {
            log.error("❌ Error connecting to Redis:");
            log.warn("Exception: ", e);
        }
    }
}
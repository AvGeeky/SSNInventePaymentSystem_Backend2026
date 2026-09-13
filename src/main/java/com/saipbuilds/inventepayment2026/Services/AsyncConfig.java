package com.saipbuilds.inventepayment2026.Services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "verificationPollerExecutor")
    public Executor verificationPollerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("verificationPollerWorker-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "reminderPollerExecutor")
    public Executor reminderPollerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("reminderPollerWorker-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "rejectionPollerExecutor")
    public Executor rejectionPollerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("rejectionPollerWorker-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "receiptOCRPollerExecutor")
    public Executor receiptOCRPollerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("receiptOCRPollerWorker-");
        executor.initialize();
        return executor;
    }
}
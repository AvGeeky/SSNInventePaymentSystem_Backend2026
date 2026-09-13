package com.saipbuilds.inventepayment2026.DBConfig.Redis;

import com.saipbuilds.inventepayment2026.Services.PaymentEmailWorker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfig {

    private final StringRedisTemplate redisTemplate;
    private final PaymentEmailWorker paymentEmailWorker;

    public static final String STREAM_KEY = "invente:payments:verified_stream";
    public static final String CONSUMER_GROUP = "email-workers-group";

    public static final String NODE_STREAM_KEY = "invente:payments:node_ocr_stream";
    public static final String NODE_CONSUMER_GROUP = "node-service-group";

    @PostConstruct
    public void initializeExternalStreams() {
        try {
            redisTemplate.opsForStream().createGroup(NODE_STREAM_KEY, ReadOffset.from("0"), NODE_CONSUMER_GROUP);
            log.info("Created Redis Consumer Group for Node.js: {}", NODE_CONSUMER_GROUP);
        } catch (Exception e) {
            log.debug("Consumer group {} already exists or stream not yet initialized.", NODE_CONSUMER_GROUP);
        }
    }

    @Bean
    public Subscription emailWorkerSubscription(RedisConnectionFactory connectionFactory) {
        // 1. Create the consumer group if it doesn't exist
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP);
            log.info("Created Redis Consumer Group: {}", CONSUMER_GROUP);
        } catch (Exception e) {
            // Group already exists, which is fine
            log.debug("Consumer group {} already exists.", CONSUMER_GROUP);
        }

        // 2. Create a dedicated 5-10 thread executor for the EMAIL workers
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("EmailWorker-");
        executor.initialize();

        // 3. Configure the listener container
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .executor(executor)
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                StreamMessageListenerContainer.create(connectionFactory, options);

        // 4. Bind the worker to the stream and consumer group
        Subscription subscription = listenerContainer.receive(
                Consumer.from(CONSUMER_GROUP, "worker-node-1"), // Node name (scales horizontally if you run multiple instances)
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                paymentEmailWorker
        );

        listenerContainer.start();
        return subscription;
    }
}
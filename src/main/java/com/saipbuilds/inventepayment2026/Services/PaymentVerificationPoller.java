package com.saipbuilds.inventepayment2026.Services;

import com.saipbuilds.inventepayment2026.DBConfig.Redis.RedisConfig;
import com.saipbuilds.inventepayment2026.mappings.VerificationPollingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentVerificationPoller {

    private final VerificationPollingMapper pollingMapper;
//    private final RedisConnectionFactory redisConnectionFactory;
//    RedisConnection redis = redisConnectionFactory.getConnection();


    // Runs every 5 seconds, but waits for the previous execution to finish first
    @Scheduled(fixedDelay = 5000) 
    @Transactional
    public void pollAndPublishVerifiedPayments() {
        
        // 1. Fetch and Lock a batch of up to 100 records
        List<Map<String, Object>> lockedBatch = pollingMapper.fetchLockedBatch(100);

        if (lockedBatch.isEmpty()) {
            return; // Nothing to do, sleep until next cycle
        }

        log.info("Polled {} newly verified payments for processing.", lockedBatch.size());

        for (Map<String, Object> row : lockedBatch) {
            UUID ticketId = (UUID) row.get("ticket_id");

            System.out.println(ticketId);

            
            // 2. Push payload to Redis Streams here


            // 3. Mark as queued in the database so the next polling cycle ignores it
            pollingMapper.markAsQueued(ticketId);
        }
        
        // Transaction commits here, locks are released, and rows are now marked 'queued'
    }
}
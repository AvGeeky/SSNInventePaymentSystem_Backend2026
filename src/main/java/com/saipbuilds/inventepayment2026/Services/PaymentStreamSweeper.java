package com.saipbuilds.inventepayment2026.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStreamSweeper {

    private final StringRedisTemplate redisTemplate;
    
    // We inject existing worker so we don't have to rewrite the email/DB logic
    private final PaymentEmailWorker paymentEmailWorker;

    private static final String STREAM_KEY = "invente:payments:verified_stream";
    private static final String CONSUMER_GROUP = "email-workers-group";
    private static final String SWEEPER_NODE = "sweeper-node";

    // Runs every 5 minutes (300,000 milliseconds)
    @Scheduled(fixedDelay = 60)
    public void reclaimStuckMessages() {
        
        // 1. Query the PEL for up to 100 pending messages
        PendingMessages pendingMessages = redisTemplate.opsForStream().pending(
                STREAM_KEY,
                CONSUMER_GROUP,
                Range.unbounded(),
                100L
        );

        if (pendingMessages.isEmpty()) {
            return;
        }

        // 2. Filter for messages that have been stuck for more than 5 minutes
        List<RecordId> stuckRecordIds = pendingMessages.stream()
                .filter(msg -> msg.getElapsedTimeSinceLastDelivery().toMinutes() >= 5)
                .map(PendingMessage::getId)
                .toList();

        if (stuckRecordIds.isEmpty()) {
            return;
        }

        log.warn("Sweeper found {} stuck messages. Attempting XCLAIM...", stuckRecordIds.size());

        // 3. Forcibly claim ownership of these messages from the dead workers
        List<MapRecord<String, Object, Object>> claimedRecords = redisTemplate.opsForStream().claim(
                STREAM_KEY,
                CONSUMER_GROUP,
                SWEEPER_NODE,
                Duration.ofMinutes(5),
                stuckRecordIds.toArray(new RecordId[0])
        );

        // 4. Reprocess the claimed messages
        for (MapRecord<String, Object, Object> rawRecord : claimedRecords) {
            try {
                // Spring Data Redis claim() returns generic Objects. 
                // We cast them back to Strings to match your PaymentEmailWorker signature safely.
                Map<String, String> stringPayload = new HashMap<>();
                rawRecord.getValue().forEach((k, v) -> stringPayload.put(String.valueOf(k), String.valueOf(v)));

                MapRecord<String, String, String> typedRecord = MapRecord.create(
                        rawRecord.getStream(),
                        stringPayload
                ).withId(rawRecord.getId());

                log.info("Sweeper routing recovered ticket to worker logic...");
                
                // Send it directly to your existing worker function
                paymentEmailWorker.onMessage(typedRecord);

            } catch (Exception e) {
                log.error("Sweeper failed to process recovered record {}", rawRecord.getId(), e);
            }
        }
    }
}
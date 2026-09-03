package com.saipbuilds.inventepayment2026.Services;

import com.saipbuilds.inventepayment2026.mappings.VerificationPollingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentVerificationPoller {

    private final VerificationPollingMapper pollingMapper;
    private final StringRedisTemplate redisTemplate;

    private static final int DAILY_EMAIL_LIMIT = 1000;
    private static final int MAX_BATCH_SIZE = 100;
    private static final String STREAM_KEY = "invente:payments:verified_stream";

    // Fires every 2 seconds. Because of @Async, if thread 1 is still working,
    // thread 2 will spawn and grab the next batch simultaneously.
    @Async("pollerExecutor")
    @Scheduled(fixedRate = 2000)
    @Transactional
    public void pollAndPublishVerifiedPayments() {

        String todayKey = "emails_sent:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String currentCountStr = redisTemplate.opsForValue().get(todayKey);
        int currentCount = (currentCountStr != null) ? Integer.parseInt(currentCountStr) : 0;

        int remainingQuota = DAILY_EMAIL_LIMIT - currentCount;

        if (remainingQuota <= 0) {

            log.trace("Daily email limit of {} reached. Poller sleeping.", DAILY_EMAIL_LIMIT);
            return;
        }

        int fetchSize = Math.min(MAX_BATCH_SIZE, remainingQuota);

        List<Map<String, Object>> lockedBatch = pollingMapper.fetchLockedBatch(fetchSize);

        if (lockedBatch.isEmpty()) {
            return;
        }

        log.info("Thread {} polled {} newly verified payments.", Thread.currentThread().getName(), lockedBatch.size());

        for (Map<String, Object> row : lockedBatch) {
            UUID ticketId = (UUID) row.get("ticket_id");

            Map<String, String> streamPayload = new HashMap<>();
            streamPayload.put("ticket_id", ticketId.toString());
            streamPayload.put("user_id", row.get("user_id").toString());
            streamPayload.put("ticket_type", row.get("ticket_type").toString());

            RecordId recordId = redisTemplate.opsForStream().add(STREAM_KEY, streamPayload);

            pollingMapper.markAsQueued(ticketId);
        }

        Long newCount = redisTemplate.opsForValue().increment(todayKey, lockedBatch.size());
        if (newCount != null && newCount == lockedBatch.size()) {
            redisTemplate.expire(todayKey, java.time.Duration.ofHours(24));
        }
    }
}
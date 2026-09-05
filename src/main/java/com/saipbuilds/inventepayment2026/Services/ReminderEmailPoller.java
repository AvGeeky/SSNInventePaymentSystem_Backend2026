package com.saipbuilds.inventepayment2026.Services;

import com.saipbuilds.inventepayment2026.mappings.TicketPaymentsMapping;
import com.saipbuilds.inventepayment2026.mappings.UsersMapping;
import com.saipbuilds.inventepayment2026.mappings.VerificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderEmailPoller {

    private final VerificationMapper verificationMapper;
    private final StringRedisTemplate redisTemplate;

    private static final int DAILY_EMAIL_LIMIT = 1000;
    private static final int MAX_BATCH_SIZE = 20;
    private static final String STREAM_KEY = "invente:payments:verified_stream";
    private final TicketPaymentsMapping ticketPaymentsMapping;
    private final UsersMapping usersMapping;

    @Async("reminderPollerExecutor")
    @Scheduled(fixedRateString = "${REMINDER_POLLER_RATE:2000}")
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

        List<Map<String, Object>> lockedBatch = verificationMapper.fetchReminderSendableLockedBatch(fetchSize);

        if (lockedBatch.isEmpty()) {
            return;
        }

        log.info("Thread {} polled {} newly verified payments.", Thread.currentThread().getName(), lockedBatch.size());

        List<Map<String, String>> payloadsToPublish = new ArrayList<>();

        for (Map<String, Object> row : lockedBatch) {
            UUID ticketId = (UUID) row.get("ticket_id");
            String recipientEmail = (String) row.get("email");


            Map<String, String> streamPayload = new HashMap<>();
            streamPayload.put("ticket_id", ticketId.toString());
            streamPayload.put("recipient_email", recipientEmail);
            streamPayload.put("email_type", "payment_reminder");

            payloadsToPublish.add(streamPayload);

            ticketPaymentsMapping.markReminderEmailAsQueued(ticketId);
        }



        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (Map<String, String> payload : payloadsToPublish) {
                            redisTemplate.opsForStream().add(STREAM_KEY, payload);
                        }
                        Long newCount = redisTemplate.opsForValue().increment(todayKey, lockedBatch.size());
                        if (newCount != null && newCount == lockedBatch.size()) {
                            redisTemplate.expire(todayKey, java.time.Duration.ofHours(24));
                        }
                    }
                }
        );
    }
}
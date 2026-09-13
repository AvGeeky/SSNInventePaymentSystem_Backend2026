package com.saipbuilds.inventepayment2026.Services;

import com.saipbuilds.inventepayment2026.DBConfig.Redis.RedisStreamConfig;
import com.saipbuilds.inventepayment2026.mappings.TicketPaymentsMapping;
import com.saipbuilds.inventepayment2026.mappings.UsersMapping;
import com.saipbuilds.inventepayment2026.mappings.VerificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
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
public class ReceiptOCRPoller {

    private final VerificationMapper verificationMapper;
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_BATCH_SIZE = 20;
    private static final String STREAM_KEY = "invente:payments:node_ocr_stream";
    private final TicketPaymentsMapping ticketPaymentsMapping;
    private final UsersMapping usersMapping;

    @Async("receiptOCRPollerExecutor")
    @Scheduled(fixedRateString = "${OCR_POLLER_RATE:20000}")
    @Transactional
    public void pollAndPublishVerifiedPayments() {

        List<Map<String, Object>> lockedBatch = verificationMapper.fetchOCRReadyRecords(MAX_BATCH_SIZE);

        if (lockedBatch.isEmpty()) {
            return;
        }

        log.info("OCR Thread {} polled {} newly verified payments.", Thread.currentThread().getName(), lockedBatch.size());

        List<Map<String, String>> payloadsToPublish = new ArrayList<>();

        for (Map<String, Object> row : lockedBatch) {
            UUID ticketId = (UUID) row.get("ticket_id");
            String pdfUrl = (String) row.get("pdf_url");


            Map<String, String> streamPayload = new HashMap<>();
            streamPayload.put("ticket_id", ticketId.toString());
            streamPayload.put("pdfUrl", pdfUrl);
            streamPayload.put("action_type", "ocr");

            payloadsToPublish.add(streamPayload);

            ticketPaymentsMapping.markPaymentIdAsQueued(ticketId);
        }





        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (Map<String, String> payload : payloadsToPublish) {
                            redisTemplate.opsForStream().add(STREAM_KEY, payload);
                        }
                    }
                }
        );
    }
}
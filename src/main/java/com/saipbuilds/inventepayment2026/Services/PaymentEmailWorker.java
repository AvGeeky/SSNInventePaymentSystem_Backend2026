package com.saipbuilds.inventepayment2026.Services;

import com.saipbuilds.inventepayment2026.mappings.TicketPaymentsMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

import static java.lang.Thread.sleep;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEmailWorker implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate redisTemplate;
    private final TicketPaymentsMapping ticketPaymentsMapping;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String recordId = message.getId().getValue();
        Map<String, String> payload = message.getValue();
        UUID ticketId = UUID.fromString(payload.get("ticket_id"));

        try {
            // ==========================================
            // 1. DO YOUR HEAVY LIFTING HERE
            // Generate QR Code, Generate PDF, Send Email
            // ==========================================

            // 2. Mark as fully complete
            ticketPaymentsMapping.updateEmailSentStatus(ticketId, "sent");

            // 3. XACK back to Redis
            redisTemplate.opsForStream().acknowledge("invente:payments:verified_stream", "email-workers-group", recordId);
            log.info("Successfully processed and XACKed ticket {}", ticketId);

        } catch (Exception e) {
            log.error("Failed to process email for ticket {}. Message left in PEL for Sweeper. Error: {}", ticketId, e.getMessage());
        }
    }
}
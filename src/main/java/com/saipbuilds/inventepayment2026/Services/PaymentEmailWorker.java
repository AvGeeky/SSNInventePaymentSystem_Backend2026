package com.saipbuilds.inventepayment2026.Services;

import com.saipbuilds.inventepayment2026.mappings.TicketPaymentsMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

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
        String userId = payload.get("user_id");
        String ticketType = payload.get("ticket_type");

        log.info("Worker {} processing ticket {} from stream.", Thread.currentThread().getName(), ticketId);

        try {
            
            // ==========================================
            // 1. DO YOUR HEAVY LIFTING HERE (NO DB LOCKS)
            // Generate QR Code, Generate PDF, Send Email
            // ==========================================
            
            
            // 2. Safely update the database (Fast, short transaction)
            markEmailAsSent(ticketId);

            // 3. XACK (Acknowledge) back to Redis so it removes the message from the pending list
            redisTemplate.opsForStream().acknowledge("invente:payments:verified_stream", "email-workers-group", recordId);
            log.debug("Successfully XACKed record {} for ticket {}", recordId, ticketId);

        } catch (Exception e) {
            log.error("Failed to process email for ticket {}. Message remains in PEL for retry. Error: {}", ticketId, e.getMessage());
            // Do NOT XACK here. If you crash, the un-ACKed message stays in Redis,
            // where a dead-letter worker or retry mechanism can claim it later.
        }
    }

    // Keep the @Transactional boundary strictly around the database update
    @Transactional
    public void markEmailAsSent(UUID ticketId) {
        ticketPaymentsMapping.updateEmailSentStatus(ticketId, "sent");
    }
}
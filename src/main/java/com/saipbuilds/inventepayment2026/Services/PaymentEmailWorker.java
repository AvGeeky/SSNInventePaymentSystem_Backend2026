package com.saipbuilds.inventepayment2026.Services;

import com.saipbuilds.inventepayment2026.TicketEmailService.TicketEmailSenderService;
import com.saipbuilds.inventepayment2026.mappings.EmailDataMapper;
import com.saipbuilds.inventepayment2026.mappings.TicketPaymentsMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEmailWorker implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate redisTemplate;
    private final TicketPaymentsMapping ticketPaymentsMapping;
    private final EmailDataMapper emailDataMapper;
    private final TicketEmailSenderService emailSenderService;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String recordId = message.getId().getValue();
        UUID ticketId = UUID.fromString(message.getValue().get("ticket_id"));

        try {
            // 1. Fetch exact user and payment data
            Map<String, Object> paymentDetails = emailDataMapper.getUserAndPaymentDetails(ticketId);

            if (paymentDetails == null) {
                log.error("Aborting: No user data found for ticket {}", ticketId);
                redisTemplate.opsForStream().acknowledge("invente:payments:verified_stream", "email-workers-group", recordId);
                return;
            }

            // 2. Check if this is a Hackathon Ticket
            Map<String, Object> teamDetails = emailDataMapper.getHackathonTeamDetails(ticketId);

            if (teamDetails != null) {
                // HACKATHON ROUTE
                UUID teamId = (UUID) teamDetails.get("team_id");
                List<Map<String, Object>> members = emailDataMapper.getHackathonMembers(teamId);

                emailSenderService.sendHackathonTicketMail(paymentDetails, teamDetails, members, ticketId);
            } else {
                // STANDARD TICKET ROUTE
                List<Map<String, Object>> bookedEvents = emailDataMapper.getEventsForTicket(ticketId);
                emailSenderService.sendTicketPurchaseMail(paymentDetails, bookedEvents, ticketId);
            }

            // 3. Mark as fully complete in database
            ticketPaymentsMapping.updateEmailSentStatus(ticketId, "sent");

            // 4. XACK back to Redis
            redisTemplate.opsForStream().acknowledge("invente:payments:verified_stream", "email-workers-group", recordId);
            log.info("Successfully processed and XACKed ticket {}", ticketId);

        } catch (Exception e) {
            log.error("Failed to process email for ticket {}. Message left in PEL for Sweeper. Error: {}", ticketId, e.getMessage(), e);
        }
    }
}
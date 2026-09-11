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
        Map<String, String> payload = message.getValue();


        String emailType = payload.getOrDefault("email_type", "tech");
        UUID ticketId = UUID.fromString(payload.getOrDefault("ticket_id", String.valueOf(UUID.randomUUID())));

        if ("send_stats_email".equals(emailType)) {
            String recipientEmail = payload.get("recipient_email");

            log.info("Worker initiating database compilation for stats email to {}", recipientEmail);

            Map<String, Object> paymentStats = emailDataMapper.getPivotedPaymentStatusStats();
            List<Map<String, Object>> eventStats = emailDataMapper.getEventWiseRegistrations();

            List<Map<String, Object>> hackStats = emailDataMapper.getHackathonDomainStats();
            List<Map<String, Object>> revenueStats = emailDataMapper.getTicketTypeRevenueStats();
            Long uniqueUsers = emailDataMapper.getUniquePaidUsersCount();
            List<Map<String, Object>> demographics = emailDataMapper.getCollegeDemographics();

            emailSenderService.sendStatsEmail(recipientEmail, paymentStats, eventStats, hackStats, revenueStats, uniqueUsers, demographics);

            redisTemplate.opsForStream().acknowledge("invente:payments:verified_stream", "email-workers-group", recordId);
            return;
        }

        try {

            // 1. Fetch User Data First (Required for all emails)
            Map<String, Object> paymentDetails = emailDataMapper.getUserAndPaymentDetails(ticketId);

            if (paymentDetails == null) {
                log.error("Aborting: No user data found for ticket {}", ticketId);
                redisTemplate.opsForStream().acknowledge("invente:payments:verified_stream", "email-workers-group", recordId);
                return;
            }

            // 2. Fetch Nested Data (Events or Hackathon Team) based on DB ticket_type
            String actualTicketType = (String) paymentDetails.getOrDefault("ticket_type", "");
            boolean isHackathon = actualTicketType.toLowerCase().contains("hackathon");

            Map<String, Object> teamDetails = null;
            List<Map<String, Object>> members = null;
            List<Map<String, Object>> bookedEvents = null;

            if (isHackathon) {
                teamDetails = emailDataMapper.getHackathonTeamDetails(ticketId);
                if (teamDetails != null) {
                    UUID teamId = (UUID) teamDetails.get("team_id");
                    members = emailDataMapper.getHackathonMembers(teamId);
                }
            } else {
                bookedEvents = emailDataMapper.getEventsForTicket(ticketId);
            }

            // 3. Route Execution

            if ("payment_reminder".equals(emailType)) {

                String recipientEmail = payload.get("recipient_email");

                emailSenderService.sendPaymentReminderMail(recipientEmail, ticketId, paymentDetails, teamDetails, members, bookedEvents);

                int s = ticketPaymentsMapping.updateReminderEmailSentStatus(ticketId);
                if (s == 1) {
                    log.info("Successfully updated reminder_email_sent status for ticket {}", ticketId);
                } else {
                    throw new Exception("Failed to update reminder_email_sent status for ticket " + ticketId);
                }
                redisTemplate.opsForStream().acknowledge("invente:payments:verified_stream", "email-workers-group", recordId);
                return;
            }
            else if ("payment_rejection".equals(emailType)) {
                String recipientEmail = payload.get("recipient_email");
                emailSenderService.sendPaymentRejectionMail(recipientEmail, ticketId);

                int s = ticketPaymentsMapping.updateRejectionEmailSentStatus(ticketId);
                if (s == 1) {
                    log.info("Successfully updated rejection_email_sent status for ticket {}", ticketId);
                } else {
                    throw new Exception("Failed to update rejection_email_sent status for ticket " + ticketId);
                }
                redisTemplate.opsForStream().acknowledge("invente:payments:verified_stream", "email-workers-group", recordId);
                return;
            }

            // Standard Ticket Purchases
            if ("hack".equals(emailType) || isHackathon) {
                if (teamDetails != null) {
                    emailSenderService.sendHackathonTicketMail(paymentDetails, teamDetails, members, ticketId);
                }
            } else {
                emailSenderService.sendTicketPurchaseMail(paymentDetails, bookedEvents, ticketId);
            }

            int s = ticketPaymentsMapping.updateEmailSentStatus(ticketId);
            if (s == 1) {
                log.info("Successfully updated email_sent status for ticket {}", ticketId);
            } else {
                throw new Exception("Failed to update email_sent status for ticket " + ticketId);
            }

            redisTemplate.opsForStream().acknowledge("invente:payments:verified_stream", "email-workers-group", recordId);
            log.info("Successfully processed and XACKed final ticket {}", ticketId);

        } catch (Exception e) {
            log.error("Failed to process email for ticket {}. Message left in PEL for Sweeper. Error: {}", ticketId, e.getMessage(), e);
        }
    }
}
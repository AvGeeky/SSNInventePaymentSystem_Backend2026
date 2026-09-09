package com.saipbuilds.inventepayment2026.Services;

import com.github.f4b6a3.uuid.UuidCreator;
import com.saipbuilds.inventepayment2026.dto.HackathonRegistrationRequest;
import com.saipbuilds.inventepayment2026.dto.StandardRegistrationRequest;
import com.saipbuilds.inventepayment2026.entities.HackathonRegs;
import com.saipbuilds.inventepayment2026.entities.HackathonMembers;
import com.saipbuilds.inventepayment2026.entities.TicketEvent;
import com.saipbuilds.inventepayment2026.entities.TicketPayments;
import com.saipbuilds.inventepayment2026.entities.Users;
import com.saipbuilds.inventepayment2026.mappings.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalControllerReceiverService {

    private final UsersMapping usersMapping;
    private final TicketPaymentsMapping ticketPaymentsMapping;
    private final TicketEventMapping ticketEventMapping;
    private final HackathonRegsMapping hackathonRegsMapping;
    private final HackathonMembersMapping hackathonMembersMapping;
    private final EventsMapping eventsMapping;

    private final StringRedisTemplate redisTemplate;

    private UUID createUUIDV7() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    @Transactional
    public UUID handleStandardRegistration(StandardRegistrationRequest request) {
        UUID newTicketId = createUUIDV7();
        UUID targetUserId;

        Users existingUser = usersMapping.findByEmail(request.getEmail());

        if (existingUser != null) {
            targetUserId = existingUser.getUserId();
        } else {
            targetUserId = createUUIDV7();
            Users newUser = Users.builder()
                    .userId(targetUserId)
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .name(request.getName())
                    .gender(request.getGender())
                    .collegeName(request.getCollegeName())
                    .yearOfStudy(request.getYearOfStudy())
                    .build();
            usersMapping.insert_users(newUser);
        }

        TicketPayments payment = TicketPayments.builder()
                .ticketId(newTicketId)
                .userId(targetUserId)
                .ticketType(request.getTicketType())
                .amountPaid(request.getAmountToBePaid())
                .status("PendingPayment")
                .build();
        ticketPaymentsMapping.insert_ticket_payment(payment);

        if (request.getEventIds() != null && !request.getEventIds().isEmpty()) {
            for (UUID eventId : request.getEventIds()) {
                TicketEvent ticketEvent = TicketEvent.builder()
                        .ticketId(newTicketId)
                        .eventId(eventId)
                        .build();
                ticketEventMapping.insert_ticket_event(ticketEvent);
            }
        }

        return newTicketId;
    }

    @Transactional
    public UUID handleHackathonRegistration(HackathonRegistrationRequest request) {
        Map<String,Object> hackStats = getHackathonStats();
        if (hackStats.get("total_registrations") != null && (Long) hackStats.get("total_registrations") > 50) {
            throw new IllegalStateException("Hackathon registration limit reached. No more registrations are allowed.");
        } else if (hackStats.get("software_count") != null && (Long) hackStats.get("software_count") > 30 && "Software".equalsIgnoreCase(request.getDomain())) {
            throw new IllegalStateException("Software Hackathon registration limit reached. No more registrations are allowed for this domain.");
        } else if (hackStats.get("hardware_count") != null && (Long) hackStats.get("hardware_count") > 20 && "Hardware".equalsIgnoreCase(request.getDomain())) {
            throw new IllegalStateException("Hardware Hackathon registration limit reached. No more registrations are allowed for this domain.");
        }

        UUID newTicketId = createUUIDV7();
        UUID newTeamId = createUUIDV7();
        UUID eventId = eventsMapping.retrieveEventIDForHackathon();
        UUID leaderUserId;

        Users existingLeader = usersMapping.findByEmail(request.getLeader().getEmail());

        if (existingLeader != null) {
            leaderUserId = existingLeader.getUserId();
        } else {
            leaderUserId = createUUIDV7();
            Users newLeader = Users.builder()
                    .userId(leaderUserId)
                    .email(request.getLeader().getEmail())
                    .phone(request.getLeader().getPhone())
                    .name(request.getLeader().getName())
                    .gender(request.getLeader().getGender())
                    .collegeName(request.getLeader().getCollegeName())
                    .yearOfStudy(request.getLeader().getYearOfStudy())
                    .build();
            usersMapping.insert_users(newLeader);
        }

        TicketPayments payment = TicketPayments.builder()
                .ticketId(newTicketId)
                .userId(leaderUserId)
                .ticketType("HACKATHON")
                .amountPaid(request.getAmountToBePaid())
                .status("PendingPayment")
                .build();
        ticketPaymentsMapping.insert_ticket_payment(payment);

        TicketEvent ticketEvent = TicketEvent.builder()
                .ticketId(newTicketId)
                .eventId(eventId)
                .build();
        ticketEventMapping.insert_ticket_event(ticketEvent);

        HackathonRegs team = HackathonRegs.builder()
                .teamId(newTeamId)
                .teamName(request.getTeamName())
                .ticketId(newTicketId)
                .domain(request.getDomain())
                .track(request.getTrack())
                .psDescription(request.getPsDescription())
                .build();
        hackathonRegsMapping.insert_hackathon_regs(team);

        HackathonMembers leaderMember = HackathonMembers.builder()
                .memberId(UUID.randomUUID())
                .teamId(newTeamId)
                .isLead(true)
                .name(request.getLeader().getName())
                .email(request.getLeader().getEmail())
                .phno(request.getLeader().getPhone())
                .yearOfStudy(request.getLeader().getYearOfStudy())
                .build();
        hackathonMembersMapping.insert_hackathon_member(leaderMember);

        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
            for (HackathonRegistrationRequest.MemberDTO teammateDto : request.getMembers()) {
                HackathonMembers teammate = HackathonMembers.builder()
                        .memberId(UUID.randomUUID())
                        .teamId(newTeamId)
                        .isLead(false)
                        .name(teammateDto.getName())
                        .email(teammateDto.getEmail())
                        .phno(teammateDto.getPhone())
                        .yearOfStudy(teammateDto.getYearOfStudy())
                        .build();
                hackathonMembersMapping.insert_hackathon_member(teammate);
            }
        }



        return newTicketId;
    }

    @Transactional
    public void updateReceiptUrl(UUID ticketId, String s3Url) {
        int updatedRows = ticketPaymentsMapping.updateReceiptUrl(ticketId, s3Url);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("Ticket ID not found.");
        }
    }

    public List<Map<String, Object>> getAllEvents() {
        return eventsMapping.getAllEvents();
    }

    public Map<String, Object> getHackathonStats() {
        return hackathonRegsMapping.getHackathonStats();
    }
}
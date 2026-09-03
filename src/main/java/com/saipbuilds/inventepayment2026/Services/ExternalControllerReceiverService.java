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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private UUID createUUIDV7() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    @Transactional
    public UUID handleStandardRegistration(StandardRegistrationRequest request) {
        // 1 Generate core Ticket UUID v7
        UUID newTicketId = createUUIDV7();
        UUID targetUserId;

        // 2 Check if user exists by email
        Users existingUser = usersMapping.findByEmail(request.getEmail());

        if (existingUser != null) {
            // User exists, reuse their UUID
            targetUserId = existingUser.getUserId();
        } else {
            // User does not exist, create UUID and insert
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

        // 3 Map DTO to TicketPayments Entity and Insert using targetUserId
        TicketPayments payment = TicketPayments.builder()
                .ticketId(newTicketId)
                .userId(targetUserId)
                .ticketType(request.getTicketType())
                .amountPaid(request.getAmountToBePaid())
                .status("PendingPayment")
                .build();
        ticketPaymentsMapping.insert_ticket_payment(payment);

        // 4 Map Event IDs to Junction Table and Insert
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
        // 1 Generate core UUIDs
        UUID newTicketId = createUUIDV7();
        UUID newTeamId = createUUIDV7();
        UUID eventId = eventsMapping.retrieveEventIDForHackathon();
        UUID leaderUserId;

        // 2 Check if the Leader exists by email
        Users existingLeader = usersMapping.findByEmail(request.getLeader().getEmail());

        if (existingLeader != null) {
            // Leader exists, reuse their UUID
            leaderUserId = existingLeader.getUserId();
        } else {
            // Leader does not exist, create UUID and insert
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

        // 3 Map & Insert the Payment Record using leaderUserId
        TicketPayments payment = TicketPayments.builder()
                .ticketId(newTicketId)
                .userId(leaderUserId) // FK linking back to the leader
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

        // 4 Map & Insert Hackathon Team Details
        HackathonRegs team = HackathonRegs.builder()
                .teamId(newTeamId)
                .teamName(request.getTeamName())
                .ticketId(newTicketId) // FK linking team to the payment
                .domain(request.getDomain())
                .track(request.getTrack())
                .psDescription(request.getPsDescription())
                .build();
        hackathonRegsMapping.insert_hackathon_regs(team);

        // 5 Insert the Leader into the informational members table
        HackathonMembers leaderMember = HackathonMembers.builder()
                .memberId(UUID.randomUUID())
                .teamId(newTeamId)
                .isLead(true) // true for the leader
                .name(request.getLeader().getName())
                .email(request.getLeader().getEmail())
                .phno(request.getLeader().getPhone())
                .yearOfStudy(request.getLeader().getYearOfStudy())
                .build();
        hackathonMembersMapping.insert_hackathon_member(leaderMember);

        // 6 Loop and Insert the remaining teammates
        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
            for (HackathonRegistrationRequest.MemberDTO teammateDto : request.getMembers()) {
                HackathonMembers teammate = HackathonMembers.builder()
                        .memberId(UUID.randomUUID())
                        .teamId(newTeamId)
                        .isLead(false) // Explicitly false for teammates
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
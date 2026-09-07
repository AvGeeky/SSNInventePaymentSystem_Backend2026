package com.saipbuilds.inventepayment2026.Services;

import com.github.f4b6a3.uuid.UuidCreator;
import com.saipbuilds.inventepayment2026.dto.HackathonRegistrationRequest;
import com.saipbuilds.inventepayment2026.dto.StandardRegistrationRequest;
import com.saipbuilds.inventepayment2026.entities.*;
import com.saipbuilds.inventepayment2026.mappings.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalControllerReceiverService {

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
    public boolean setPaymentVerified(UUID ticketId) {
        // Check if the ticket exists
        String time = ticketPaymentsMapping.findTicket(ticketId);
        if (time == null) {
            return false;
        }
        int r = ticketPaymentsMapping.updateStatusToAcceptedForSpecificTicket(ticketId);
        return r == 1;
    }

    public boolean setPaymentRejected( UUID ticketId) {
        // Implementation for rejecting payment
        String time = ticketPaymentsMapping.findTicket(ticketId);
        if (time == null) {
            return false;
        }
        int r = ticketPaymentsMapping.updateStatusToRejectedForSpecificTicket(ticketId);
        return r == 1;
    }
}
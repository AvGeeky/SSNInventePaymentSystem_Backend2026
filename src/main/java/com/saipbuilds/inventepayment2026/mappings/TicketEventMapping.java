package com.saipbuilds.inventepayment2026.mappings;

import com.saipbuilds.inventepayment2026.entities.TicketEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketEventMapping {
    @Insert("INSERT INTO invente_payment_db.public.ticket_event (ticket_id, event_id) " +
            "VALUES (#{ticketId}, #{eventId})")
    int insert_ticket_event(TicketEvent ticketEvent);
}
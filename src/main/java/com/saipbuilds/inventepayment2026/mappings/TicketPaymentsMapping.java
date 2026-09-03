package com.saipbuilds.inventepayment2026.mappings;

import com.saipbuilds.inventepayment2026.entities.TicketPayments;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketPaymentsMapping {
    @Insert("INSERT INTO invente_payment_db.public.ticket_payments (ticket_id, user_id, ticket_type, amount_paid, status) " +
            "VALUES (#{ticketId}, #{userId}, #{ticketType}, #{amountPaid}, #{status})")
    int insert_ticket_payment(TicketPayments payment);
}

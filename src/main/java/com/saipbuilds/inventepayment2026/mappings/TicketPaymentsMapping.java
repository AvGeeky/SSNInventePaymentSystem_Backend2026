package com.saipbuilds.inventepayment2026.mappings;

import com.saipbuilds.inventepayment2026.entities.TicketPayments;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.UUID;

@Mapper
public interface TicketPaymentsMapping {
    @Insert("INSERT INTO invente_payment_db.public.ticket_payments (ticket_id, user_id, ticket_type, amount_paid, status) " +
            "VALUES (#{ticketId}, #{userId}, #{ticketType}, #{amountPaid}, #{status})")
    int insert_ticket_payment(TicketPayments payment);

    @Insert("INSERT INTO invente_payment_db.public.ticket_payments (ticket_id, user_id, ticket_type, amount_paid, status) " +
            "VALUES (#{ticketId}, #{userId}, #{ticketType}, #{amountPaid}, 'Accepted')")
    int INTERNALLY_VERIFIED_insert_ticket_payment(TicketPayments payment);

    @Update("UPDATE invente_payment_db.public.ticket_payments SET s3_url = #{s3Url}, status = 'NotVerified' WHERE ticket_id = #{ticketId}")
    int updateReceiptUrl(@Param("ticketId") UUID ticketId, @Param("s3Url") String s3Url);

    @Update("UPDATE invente_payment_db.public.ticket_payments SET email_sent = #{status} WHERE ticket_id = #{ticketId}")
    int updateEmailSentStatus(@Param("ticketId") UUID ticketId, @Param("status") String status);



}

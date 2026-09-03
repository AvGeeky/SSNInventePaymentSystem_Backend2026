package com.saipbuilds.inventepayment2026.mappings;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface VerificationPollingMapper {

    @Select("""
        SELECT ticket_id, user_id, ticket_type 
        FROM invente_payment_db.public.ticket_payments 
        WHERE status = 'Accepted' AND email_sent IS NULL 
        LIMIT #{batchSize} 
        FOR UPDATE SKIP LOCKED
    """)
    List<Map<String, Object>> fetchLockedBatch(@Param("batchSize") int batchSize);

    @Update("UPDATE invente_payment_db.public.ticket_payments SET email_sent = 'queued' WHERE ticket_id = #{ticketId}")
    int markAsQueued(@Param("ticketId") UUID ticketId);
}
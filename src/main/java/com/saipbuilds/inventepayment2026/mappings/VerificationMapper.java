package com.saipbuilds.inventepayment2026.mappings;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface VerificationMapper {

    @Select("""
        SELECT ticket_id, user_id, ticket_type\s
        FROM invente_payment_db.public.ticket_payments\s
        WHERE status = 'Accepted' AND email_sent IS NULL\s
        LIMIT #{batchSize}\s
        FOR UPDATE SKIP LOCKED
   \s""")
    List<Map<String, Object>> fetchLockedBatch(@Param("batchSize") int batchSize);


    @Select("""
        SELECT tps.ticket_id, tps.user_id, u.email 
        FROM invente_payment_db.public.ticket_payments tps 
        JOIN invente_payment_db.public.users u ON tps.user_id = u.user_id 
        WHERE tps.reminder_email_sent IS NULL 
          AND tps.s3_url IS NULL 
          AND tps.created_at <= NOW() - INTERVAL '3 minutes'
        LIMIT #{batchSize} 
        FOR UPDATE OF tps SKIP LOCKED
   """)
    List<Map<String, Object>> fetchReminderSendableLockedBatch(@Param("batchSize") int batchSize);

}
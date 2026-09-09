package com.saipbuilds.inventepayment2026.mappings;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface EmailDataMapper {

    @Select("""
        SELECT u.name, u.email, u.phone, u.college_name, t.amount_paid, t.updated_at as paid_on, t.ticket_type
        FROM invente_payment_db.public.users u
        JOIN invente_payment_db.public.ticket_payments t ON u.user_id = t.user_id
        WHERE t.ticket_id = #{ticketId}
    """)
    Map<String, Object> getUserAndPaymentDetails(@Param("ticketId") UUID ticketId);

    @Select("""
        SELECT e.event_id, e.name as event_name, e.dept_name
        FROM invente_payment_db.public.ticket_event te
        JOIN invente_payment_db.public.events e ON te.event_id = e.event_id
        WHERE te.ticket_id = #{ticketId}
    """)
    List<Map<String, Object>> getEventsForTicket(@Param("ticketId") UUID ticketId);

    @Select("""
        SELECT team_id, team_name, domain, track, ps_description
        FROM invente_payment_db.public.hackathon_regs
        WHERE ticket_id = #{ticketId}
    """)
    Map<String, Object> getHackathonTeamDetails(@Param("ticketId") UUID ticketId);

    @Select("""
        SELECT name, email, phno, year_of_study, is_lead
        FROM invente_payment_db.public.hackathon_members
        WHERE team_id = #{teamId}
        ORDER BY is_lead DESC
    """)
    List<Map<String, Object>> getHackathonMembers(@Param("teamId") UUID teamId);
}
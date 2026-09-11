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

    @Select("""
        SELECT 
            SUM(CASE WHEN status = 'PendingPayment' THEN 1 ELSE 0 END) as pending_count,
            SUM(CASE WHEN status = 'NotVerified' THEN 1 ELSE 0 END) as not_verified_count,
            SUM(CASE WHEN status = 'Accepted' THEN 1 ELSE 0 END) as accepted_count,
            SUM(CASE WHEN status = 'Rejected' THEN 1 ELSE 0 END) as rejected_count
        FROM invente_payment_db.public.ticket_payments
    """)
    Map<String, Object> getPivotedPaymentStatusStats();
    @Select("""
        SELECT domain, COUNT(team_id) as count 
        FROM invente_payment_db.public.hackathon_regs 
        WHERE ticket_id IN (SELECT ticket_id FROM invente_payment_db.public.ticket_payments WHERE status = 'Accepted' OR status = 'NotVerified')
        GROUP BY domain
    """)
    List<Map<String, Object>> getHackathonDomainStats();

    @Select("""
        SELECT ticket_type, COUNT(ticket_id) as tickets_booked, COALESCE(SUM(amount_paid), 0) as cost_recovered 
        FROM invente_payment_db.public.ticket_payments 
        WHERE status = 'Accepted' 
        GROUP BY ticket_type
    """)
    List<Map<String, Object>> getTicketTypeRevenueStats();

    @Select("""
        SELECT COUNT(DISTINCT user_id) 
        FROM invente_payment_db.public.ticket_payments 
        WHERE status = 'Accepted'
    """)
    Long getUniquePaidUsersCount();



    @Select("""
        SELECT 
            e.name AS event_name, 
            e.dept_name,
            e.event_type,
            COUNT(tp.ticket_id) AS accepted_registrations
        FROM invente_payment_db.public.events e
        LEFT JOIN invente_payment_db.public.ticket_event te ON e.event_id = te.event_id
        LEFT JOIN invente_payment_db.public.ticket_payments tp 
            ON te.ticket_id = tp.ticket_id AND tp.status = 'Accepted'
        GROUP BY e.event_id, e.name, e.dept_name, e.event_type
        ORDER BY accepted_registrations DESC, e.name ASC
    """)
    List<Map<String, Object>> getEventWiseRegistrations();







    @Select("""
        SELECT 
            college_name,
            COUNT(user_id) as total_students,
            SUM(CASE WHEN gender = 'M' THEN 1 ELSE 0 END) as male_count,
            SUM(CASE WHEN gender = 'F' THEN 1 ELSE 0 END) as female_count,
            SUM(CASE WHEN year_of_study = 1 THEN 1 ELSE 0 END) as year_1_count,
            SUM(CASE WHEN year_of_study = 2 THEN 1 ELSE 0 END) as year_2_count,
            SUM(CASE WHEN year_of_study = 3 THEN 1 ELSE 0 END) as year_3_count,
            SUM(CASE WHEN year_of_study = 4 THEN 1 ELSE 0 END) as year_4_count,
            SUM(CASE WHEN year_of_study NOT IN (1,2,3,4) THEN 1 ELSE 0 END) as year_other_count
        FROM invente_payment_db.public.users
        WHERE user_id IN (SELECT user_id FROM invente_payment_db.public.ticket_payments WHERE status = 'Accepted')
        GROUP BY college_name
        ORDER BY total_students DESC
    """)
    List<Map<String, Object>> getCollegeDemographics();

}
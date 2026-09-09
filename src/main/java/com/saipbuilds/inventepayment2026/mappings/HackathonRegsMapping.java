package com.saipbuilds.inventepayment2026.mappings;

import com.saipbuilds.inventepayment2026.entities.HackathonRegs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface HackathonRegsMapping {
    @Insert("INSERT INTO invente_payment_db.public.hackathon_regs (team_id, team_name, ticket_id, domain, track, ps_description) " +
            "VALUES (#{teamId}, #{teamName}, #{ticketId}, #{domain}, #{track}, #{psDescription})")
    int insert_hackathon_regs(HackathonRegs team);

    @Select("""
        SELECT
            COUNT(*) AS total_registrations,
            COUNT(*) FILTER (WHERE hr.domain = 'Software') AS software_count,
            COUNT(*) FILTER (WHERE hr.domain = 'Hardware') AS hardware_count
        FROM invente_payment_db.public.hackathon_regs hr
        JOIN invente_payment_db.public.ticket_payments tps
            ON hr.ticket_id = tps.ticket_id
        WHERE tps.status = 'NotVerified' OR tps.status = 'Accepted';
    """)
    Map<String, Object> getHackathonStats();
}
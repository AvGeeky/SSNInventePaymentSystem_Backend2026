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
            COUNT(*) as total_registrations, 
            COALESCE(SUM(CASE WHEN domain = 'Software' THEN 1 ELSE 0 END), 0) as software_count, 
            COALESCE(SUM(CASE WHEN domain = 'Hardware' THEN 1 ELSE 0 END), 0) as hardware_count 
        FROM invente_payment_db.public.hackathon_regs
    """)
    Map<String, Object> getHackathonStats();
}
package com.saipbuilds.inventepayment2026.mappings;

import com.saipbuilds.inventepayment2026.entities.HackathonRegs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HackathonRegsMapping {
    @Insert("INSERT INTO invente_payment_db.public.hackathon_regs (team_id, team_name, ticket_id, domain, track, ps_description) " +
            "VALUES (#{teamId}, #{teamName}, #{ticketId}, #{domain}, #{track}, #{psDescription})")
    int insert_hackathon_regs(HackathonRegs team);
}
package com.saipbuilds.inventepayment2026.mappings;

import com.saipbuilds.inventepayment2026.entities.HackathonMembers;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HackathonMembersMapping {
    @Insert("INSERT INTO invente_payment_db.public.hackathon_members (member_id, team_id, is_lead, name, email, phno, year_of_study) " +
            "VALUES (#{memberId}, #{teamId}, #{isLead}, #{name}, #{email}, #{phno}, #{yearOfStudy})")
    int insert_hackathon_member(HackathonMembers member);
}
package com.saipbuilds.inventepayment2026.mappings;

import com.saipbuilds.inventepayment2026.entities.Users;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface EventsMapping {
    @Select("SELECT event_id from invente_payment_db.public.events where event_type='HACKATHON' or name='HackInfinity'")
    UUID retrieveEventIDForHackathon();

    @Select("SELECT event_id, name, event_type, date FROM invente_payment_db.public.events ORDER BY date ASC")
    List<Map<String, Object>> getAllEvents();
}

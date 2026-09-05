package com.saipbuilds.inventepayment2026.mappings;

import com.saipbuilds.inventepayment2026.entities.Users;
import org.apache.ibatis.annotations.*;

import java.util.UUID;

@Mapper
public interface UsersMapping {
    @Insert("INSERT INTO invente_payment_db.public.users (user_id, email, phone, name, gender, college_name, year_of_study) values (#{userId}, #{email}, #{phone}, #{name}, #{gender}, #{collegeName}, #{yearOfStudy})")
    int insert_users(Users users);

    @Select("SELECT * FROM invente_payment_db.public.users WHERE email = #{email}")
    Users findByEmail(@Param("email") String email);

    @Select("SELECT email FROM invente_payment_db.public.users WHERE user_id = #{userId}")
    String findEmailByUserId(@Param("userId") UUID userId);
}
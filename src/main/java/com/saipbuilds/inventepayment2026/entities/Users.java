package com.saipbuilds.inventepayment2026.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    private UUID userId;
    private String email;
    private String phone;
    private String name;
    private String gender;
    private String collegeName;
    private Integer yearOfStudy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
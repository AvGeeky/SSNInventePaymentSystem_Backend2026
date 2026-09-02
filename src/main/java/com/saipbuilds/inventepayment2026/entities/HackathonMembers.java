package com.saipbuilds.inventepayment2026.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HackathonMembers {
    private UUID memberId;
    private UUID teamId;
    private Boolean isLead;
    private String name;
    private String email;
    private String phno;
    private Integer yearOfStudy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
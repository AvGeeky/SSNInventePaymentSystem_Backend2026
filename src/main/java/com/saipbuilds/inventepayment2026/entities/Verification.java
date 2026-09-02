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
public class Verification {
    private UUID volunteerId;
    private String email;
    private String passwordHash;
    private String dept;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
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
public class HackathonRegs {
    private UUID teamId;
    private String teamName;
    private UUID ticketId;
    private String domain;
    private String track;
    private String psDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
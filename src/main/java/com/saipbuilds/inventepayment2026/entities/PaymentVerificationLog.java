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
public class PaymentVerificationLog {
    private UUID logId;
    private UUID ticketId;
    private UUID volunteerId;
    private String actionTaken;
    private LocalDateTime verifTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
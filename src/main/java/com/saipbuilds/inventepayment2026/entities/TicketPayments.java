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
public class TicketPayments {
    private UUID ticketId;
    private UUID userId;
    private String ticketType;
    private BigDecimal amountPaid;
    private String s3Url;
    private String status;
    private String emailSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
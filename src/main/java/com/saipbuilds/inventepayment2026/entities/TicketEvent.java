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
public class TicketEvent {
    private UUID ticketId;
    private UUID eventId;
    private Boolean attendance;
    private LocalDateTime attendanceTimestamp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
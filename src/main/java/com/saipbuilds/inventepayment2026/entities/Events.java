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
public class Events {
    private UUID eventId;
    private LocalDateTime date;
    private String name;
    private String deptName;
    private Integer regCount;
    private Integer attendCount;
    private String eventType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
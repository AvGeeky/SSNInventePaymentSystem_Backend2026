package com.saipbuilds.inventepayment2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StandardRegistrationRequest {
    private String email;
    private String phone;
    private String name;
    private String gender;
    private String collegeName;
    private Integer yearOfStudy;
    private String ticketType;
    private BigDecimal amountToBePaid;
    private List<UUID> eventIds;
}
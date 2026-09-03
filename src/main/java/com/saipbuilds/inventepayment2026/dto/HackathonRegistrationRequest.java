package com.saipbuilds.inventepayment2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HackathonRegistrationRequest {
    private LeaderDTO leader;
    private String teamName;
    private String domain;
    private String track;
    private String psDescription;
    private List<MemberDTO> members;
    private BigDecimal amountToBePaid;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class LeaderDTO {
        private String email;
        private String phone;
        private String name;
        private String gender;
        private String collegeName;
        private Integer yearOfStudy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class MemberDTO {
        private String name;
        private String email;
        private String phone;
        private Integer yearOfStudy;
    }
}
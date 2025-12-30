package com.tiktel.ttelgo.apikey.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsageStatsDto {
    private Long totalRequests;
    private Long requestsToday;
    private Long requestsThisWeek;
    private Long requestsThisMonth;
    private Double averageResponseTime;
    private Long totalErrors;
    private Double errorRate;
    private List<EndpointUsageDto> topEndpoints;
    private List<StatusCodeCountDto> statusCodeDistribution;
    private List<DailyUsageDto> dailyUsage;
    private Map<String, Long> hourlyUsage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointUsageDto {
        private String endpoint;
        private Long count;
        private Double averageResponseTime;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCodeCountDto {
        private Integer statusCode;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUsageDto {
        private String date;
        private Long requests;
        private Long errors;
        private Double averageResponseTime;
    }
}


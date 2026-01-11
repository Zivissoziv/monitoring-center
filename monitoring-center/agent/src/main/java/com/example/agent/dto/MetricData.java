package com.example.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metric data transfer object for Kafka message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricData {
    
    private String agentId;
    private String metricType;
    private Double metricValue;
    private String textValue;
    private Long timestamp;
}

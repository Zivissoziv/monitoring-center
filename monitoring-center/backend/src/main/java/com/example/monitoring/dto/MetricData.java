package com.example.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metric data transfer object from Kafka
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

package com.example.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metric collection configuration from backend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricCollectionConfig {
    
    private String metricName;
    private String displayName;
    private String metricType;  // NUMERIC or BOOLEAN
    private String collectionCommand;
    private Integer collectionInterval;  // in seconds
    private String processingRule;
    private String unit;
    private Boolean enabled;
}

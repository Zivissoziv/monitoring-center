package com.example.monitoring.event;

import com.example.monitoring.entity.Metric;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a new metric is collected
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricCollectedEvent {
    private Metric metric;
    private long timestamp;
    
    public MetricCollectedEvent(Metric metric) {
        this.metric = metric;
        this.timestamp = System.currentTimeMillis();
    }
}

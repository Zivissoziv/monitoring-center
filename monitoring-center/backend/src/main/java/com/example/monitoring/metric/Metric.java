package com.example.monitoring.metric;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Metric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "agent_id")
    private String agentId;
    private String metricType; // CPU, MEMORY, etc.
    
    @Column(name = "metric_value")
    private double value;
    
    @Column(name = "text_value")
    private String textValue; // For string-type metrics
    
    private long timestamp;
    
    public Metric(String agentId, String metricType, double value) {
        this.agentId = agentId;
        this.metricType = metricType;
        this.value = value;
        this.textValue = null;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Metric(String agentId, String metricType, String textValue) {
        this.agentId = agentId;
        this.metricType = metricType;
        this.value = 0.0;
        this.textValue = textValue;
        this.timestamp = System.currentTimeMillis();
    }
}
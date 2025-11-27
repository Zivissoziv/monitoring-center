package com.example.monitoring.metric;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "metrics")
public class Metric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long agentId;
    private String metricType; // CPU, MEMORY, etc.
    
    @javax.persistence.Column(name = "metric_value")
    private double value;
    
    private long timestamp;
    
    // Constructors
    public Metric() {}
    
    public Metric(Long agentId, String metricType, double value) {
        this.agentId = agentId;
        this.metricType = metricType;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAgentId() {
        return agentId;
    }
    
    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
    
    public String getMetricType() {
        return metricType;
    }
    
    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }
    
    public double getValue() {
        return value;
    }
    
    public void setValue(double value) {
        this.value = value;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
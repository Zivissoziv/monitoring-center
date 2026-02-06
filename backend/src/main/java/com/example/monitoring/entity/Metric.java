package com.example.monitoring.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

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
    
    @Column(name = "app_code", length = 10)
    private String appCode;
    
    private String metricType; // CPU, MEMORY, etc.
    
    @Column(name = "metric_value")
    private double value;
    
    @Column(name = "text_value")
    private String textValue; // For string-type metrics
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    public Metric(String agentId, String metricType, double value) {
        this.agentId = agentId;
        this.appCode = null;
        this.metricType = metricType;
        this.value = value;
        this.textValue = null;
        this.timestamp = LocalDateTime.now();
    }
    
    public Metric(String agentId, String metricType, String textValue) {
        this.agentId = agentId;
        this.appCode = null;
        this.metricType = metricType;
        this.value = 0.0;
        this.textValue = textValue;
        this.timestamp = LocalDateTime.now();
    }
    
    public Metric(String agentId, String appCode, String metricType, double value) {
        this.agentId = agentId;
        this.appCode = appCode;
        this.metricType = metricType;
        this.value = value;
        this.textValue = null;
        this.timestamp = LocalDateTime.now();
    }
    
    public Metric(String agentId, String appCode, String metricType, String textValue) {
        this.agentId = agentId;
        this.appCode = appCode;
        this.metricType = metricType;
        this.value = 0.0;
        this.textValue = textValue;
        this.timestamp = LocalDateTime.now();
    }
}

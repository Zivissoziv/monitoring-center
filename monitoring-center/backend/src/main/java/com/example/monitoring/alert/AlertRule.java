package com.example.monitoring.alert;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "alert_rules")
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private Long agentId; // null means applies to all agents
    private String metricType; // CPU, MEMORY, etc.
    private String condition; // GT, LT, EQ (greater than, less than, equal)
    private double threshold;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private boolean enabled;
    
    // Constructors
    public AlertRule() {}
    
    public AlertRule(String name, String metricType, String condition, double threshold, String severity) {
        this.name = name;
        this.metricType = metricType;
        this.condition = condition;
        this.threshold = threshold;
        this.severity = severity;
        this.enabled = true;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public double getThreshold() {
        return threshold;
    }
    
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
package com.example.monitoring.alert;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "alert_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    @Column(name = "agent_id")
    private String agentId; // null means applies to all agents
    private String metricType; // CPU, MEMORY, etc.
    private String condition; // GT, LT, EQ, GTE, LTE (for numeric), EQUALS, NOT_EQUALS (for boolean/string), CONTAINS (for string)
    private Double threshold; // For numeric comparisons
    @Column(name = "threshold_text")
    private String thresholdText; // For boolean/string comparisons
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    @Column(name = "alert_message", length = 1000)
    private String alertMessage; // Custom alert message to display when triggered
    private boolean enabled;
    
    public AlertRule(String name, String metricType, String condition, double threshold, String severity) {
        this.name = name;
        this.metricType = metricType;
        this.condition = condition;
        this.threshold = threshold;
        this.thresholdText = null;
        this.severity = severity;
        this.enabled = true;
    }
    
    public AlertRule(String name, String metricType, String condition, String thresholdText, String severity) {
        this.name = name;
        this.metricType = metricType;
        this.condition = condition;
        this.threshold = null;
        this.thresholdText = thresholdText;
        this.severity = severity;
        this.enabled = true;
    }
}
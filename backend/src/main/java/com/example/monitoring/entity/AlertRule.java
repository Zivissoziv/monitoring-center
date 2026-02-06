package com.example.monitoring.entity;

import com.example.monitoring.enums.AlertCondition;
import com.example.monitoring.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "condition", length = 20)
    private AlertCondition condition; // GT, LT, EQ, GTE, LTE, EQUALS, NOT_EQUALS, CONTAINS
    
    private Double threshold; // For numeric comparisons
    @Column(name = "threshold_text")
    private String thresholdText; // For boolean/string comparisons
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "alert_message", length = 1000)
    private String alertMessage; // Custom alert message to display when triggered
    private boolean enabled;
    
    public AlertRule(String name, String metricType, AlertCondition condition, double threshold, Severity severity) {
        this.name = name;
        this.metricType = metricType;
        this.condition = condition;
        this.threshold = threshold;
        this.thresholdText = null;
        this.severity = severity;
        this.enabled = true;
    }
    
    public AlertRule(String name, String metricType, AlertCondition condition, String thresholdText, Severity severity) {
        this.name = name;
        this.metricType = metricType;
        this.condition = condition;
        this.threshold = null;
        this.thresholdText = thresholdText;
        this.severity = severity;
        this.enabled = true;
    }
}

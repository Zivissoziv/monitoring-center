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
    private String condition; // GT, LT, EQ (greater than, less than, equal)
    private double threshold;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private boolean enabled;
    
    public AlertRule(String name, String metricType, String condition, double threshold, String severity) {
        this.name = name;
        this.metricType = metricType;
        this.condition = condition;
        this.threshold = threshold;
        this.severity = severity;
        this.enabled = true;
    }
}
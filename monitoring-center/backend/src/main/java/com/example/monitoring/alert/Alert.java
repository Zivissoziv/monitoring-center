package com.example.monitoring.alert;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "alerts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"alert_rule_id", "agent_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "alert_rule_id")
    private Long alertRuleId;
    
    @Column(name = "agent_id")
    private String agentId;
    
    private String ruleName;
    private String metricType;
    private double triggerValue;
    private double threshold;
    private String severity;
    private String status; // ACTIVE, ACKNOWLEDGED, RESOLVED
    private long firstTriggeredAt;
    private long lastTriggeredAt;
    private int triggerCount;
    private String acknowledgedBy;
    private long acknowledgedAt;
    private String resolveNote;
    
    public Alert(Long alertRuleId, String agentId, String ruleName, String metricType, 
                 double triggerValue, double threshold, String severity) {
        this.alertRuleId = alertRuleId;
        this.agentId = agentId;
        this.ruleName = ruleName;
        this.metricType = metricType;
        this.triggerValue = triggerValue;
        this.threshold = threshold;
        this.severity = severity;
        this.status = "ACTIVE";
        this.firstTriggeredAt = System.currentTimeMillis();
        this.lastTriggeredAt = System.currentTimeMillis();
        this.triggerCount = 1;
    }
}
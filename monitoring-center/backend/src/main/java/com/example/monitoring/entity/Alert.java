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

@Entity
@Table(name = "alerts")
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
    private Double triggerValue; // For numeric metrics
    @Column(name = "trigger_value_text")
    private String triggerValueText; // For string metrics
    private Double threshold; // For numeric rules
    @Column(name = "threshold_text")
    private String thresholdText; // For boolean/string rules
    private String severity;
    @Column(name = "alert_message", length = 1000)
    private String alertMessage; // Custom message from alert rule
    private String status; // ACTIVE, ACKNOWLEDGED, RESOLVED
    private long firstTriggeredAt;
    private long lastTriggeredAt;
    private int triggerCount;
    private String acknowledgedBy;
    private long acknowledgedAt;
    private String resolveNote;
    private Long resolvedAt;
    
    @Column(name = "external_alert_id")
    private String externalAlertId; // 第三方告警的外部ID
    
    // Constructor for numeric alerts
    public Alert(Long alertRuleId, String agentId, String ruleName, String metricType, 
                 double triggerValue, double threshold, String severity) {
        this.alertRuleId = alertRuleId;
        this.agentId = agentId;
        this.ruleName = ruleName;
        this.metricType = metricType;
        this.triggerValue = triggerValue;
        this.triggerValueText = null;
        this.threshold = threshold;
        this.thresholdText = null;
        this.severity = severity;
        this.status = "ACTIVE";
        this.firstTriggeredAt = System.currentTimeMillis();
        this.lastTriggeredAt = System.currentTimeMillis();
        this.triggerCount = 1;
    }
    
    // Constructor for boolean/string alerts
    public Alert(Long alertRuleId, String agentId, String ruleName, String metricType,
                 String triggerValueText, String thresholdText, String severity) {
        this.alertRuleId = alertRuleId;
        this.agentId = agentId;
        this.ruleName = ruleName;
        this.metricType = metricType;
        this.triggerValue = null;
        this.triggerValueText = triggerValueText;
        this.threshold = null;
        this.thresholdText = thresholdText;
        this.severity = severity;
        this.status = "ACTIVE";
        this.firstTriggeredAt = System.currentTimeMillis();
        this.lastTriggeredAt = System.currentTimeMillis();
        this.triggerCount = 1;
    }
}

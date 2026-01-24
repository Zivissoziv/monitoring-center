package com.example.monitoring.entity;

import com.example.monitoring.enums.AlertStatus;
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
import java.time.LocalDateTime;

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
    
    @Column(name = "app_code", length = 10)
    private String appCode;
    
    private String ruleName;
    private String metricType;
    private Double triggerValue; // For numeric metrics
    @Column(name = "trigger_value_text")
    private String triggerValueText; // For string metrics
    private Double threshold; // For numeric rules
    @Column(name = "threshold_text")
    private String thresholdText; // For boolean/string rules
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;
    
    @Column(name = "alert_message", length = 1000)
    private String alertMessage; // Custom message from alert rule
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AlertStatus status;
    
    @Column(name = "first_triggered_at")
    private LocalDateTime firstTriggeredAt;
    
    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;
    
    private int triggerCount;
    private String acknowledgedBy;
    
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    private String resolveNote;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "external_alert_id")
    private String externalAlertId; // 第三方告警的外部ID
    
    // Constructor for numeric alerts
    public Alert(Long alertRuleId, String agentId, String ruleName, String metricType, 
                 double triggerValue, double threshold, Severity severity) {
        this.alertRuleId = alertRuleId;
        this.agentId = agentId;
        this.appCode = null;
        this.ruleName = ruleName;
        this.metricType = metricType;
        this.triggerValue = triggerValue;
        this.triggerValueText = null;
        this.threshold = threshold;
        this.thresholdText = null;
        this.severity = severity;
        this.status = AlertStatus.ACTIVE;
        this.firstTriggeredAt = LocalDateTime.now();
        this.lastTriggeredAt = LocalDateTime.now();
        this.triggerCount = 1;
    }
    
    // Constructor for boolean/string alerts
    public Alert(Long alertRuleId, String agentId, String ruleName, String metricType,
                 String triggerValueText, String thresholdText, Severity severity) {
        this.alertRuleId = alertRuleId;
        this.agentId = agentId;
        this.appCode = null;
        this.ruleName = ruleName;
        this.metricType = metricType;
        this.triggerValue = null;
        this.triggerValueText = triggerValueText;
        this.threshold = null;
        this.thresholdText = thresholdText;
        this.severity = severity;
        this.status = AlertStatus.ACTIVE;
        this.firstTriggeredAt = LocalDateTime.now();
        this.lastTriggeredAt = LocalDateTime.now();
        this.triggerCount = 1;
    }
}

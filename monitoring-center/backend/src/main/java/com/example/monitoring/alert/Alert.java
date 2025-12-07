package com.example.monitoring.alert;

import javax.persistence.*;

@Entity
@Table(name = "alerts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"alert_rule_id", "agent_id"}))
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "alert_rule_id")
    private Long alertRuleId;
    
    @Column(name = "agent_id")
    private Long agentId;
    
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
    
    // Constructors
    public Alert() {}
    
    public Alert(Long alertRuleId, Long agentId, String ruleName, String metricType, 
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
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAlertRuleId() {
        return alertRuleId;
    }
    
    public void setAlertRuleId(Long alertRuleId) {
        this.alertRuleId = alertRuleId;
    }
    
    public Long getAgentId() {
        return agentId;
    }
    
    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public String getMetricType() {
        return metricType;
    }
    
    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }
    
    public double getTriggerValue() {
        return triggerValue;
    }
    
    public void setTriggerValue(double triggerValue) {
        this.triggerValue = triggerValue;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getFirstTriggeredAt() {
        return firstTriggeredAt;
    }
    
    public void setFirstTriggeredAt(long firstTriggeredAt) {
        this.firstTriggeredAt = firstTriggeredAt;
    }
    
    public long getLastTriggeredAt() {
        return lastTriggeredAt;
    }
    
    public void setLastTriggeredAt(long lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }
    
    public int getTriggerCount() {
        return triggerCount;
    }
    
    public void setTriggerCount(int triggerCount) {
        this.triggerCount = triggerCount;
    }
    
    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }
    
    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }
    
    public long getAcknowledgedAt() {
        return acknowledgedAt;
    }
    
    public void setAcknowledgedAt(long acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }
    
    public String getResolveNote() {
        return resolveNote;
    }
    
    public void setResolveNote(String resolveNote) {
        this.resolveNote = resolveNote;
    }
}
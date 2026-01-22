package com.example.monitoring.entity;

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
@Table(name = "alert_rule_emergency")
@Data
@NoArgsConstructor
public class AlertRuleEmergency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "alert_rule_id")
    private Long alertRuleId;
    
    @Column(name = "emergency_knowledge_id")
    private Long emergencyKnowledgeId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public AlertRuleEmergency(Long alertRuleId, Long emergencyKnowledgeId) {
        this.alertRuleId = alertRuleId;
        this.emergencyKnowledgeId = emergencyKnowledgeId;
        this.createdAt = LocalDateTime.now();
    }
}

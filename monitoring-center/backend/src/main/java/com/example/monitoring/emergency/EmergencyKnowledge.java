package com.example.monitoring.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emergency_knowledge")
@Data
@NoArgsConstructor
public class EmergencyKnowledge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "alert_rule_id", unique = true)
    private Long alertRuleId; // One-to-one with AlertRule
    
    private String title; // Title of the emergency procedure
    
    @Column(length = 2000)
    private String description; // Overall description
    
    @Column(name = "created_at")
    private long createdAt;
    
    @Column(name = "updated_at")
    private long updatedAt;
    
    @Transient
    private List<EmergencyStep> steps = new ArrayList<>();
    
    public EmergencyKnowledge(Long alertRuleId, String title, String description) {
        this.alertRuleId = alertRuleId;
        this.title = title;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}

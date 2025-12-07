package com.example.monitoring.emergency;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emergency_knowledge")
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
    
    // Constructors
    public EmergencyKnowledge() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public EmergencyKnowledge(Long alertRuleId, String title, String description) {
        this.alertRuleId = alertRuleId;
        this.title = title;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<EmergencyStep> getSteps() {
        return steps;
    }
    
    public void setSteps(List<EmergencyStep> steps) {
        this.steps = steps;
    }
}

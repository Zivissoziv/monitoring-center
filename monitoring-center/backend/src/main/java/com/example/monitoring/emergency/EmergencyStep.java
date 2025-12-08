package com.example.monitoring.emergency;

import javax.persistence.*;

@Entity
@Table(name = "emergency_steps")
public class EmergencyStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "knowledge_id")
    private Long knowledgeId;
    
    @Column(name = "step_order")
    private int stepOrder; // Execution order of the step
    
    private String description; // Description of the step
    
    @Column(name = "linux_command", length = 2000)
    private String linuxCommand; // Linux command to execute
    
    @Column(name = "agent_id")
    private Long agentId; // ID of the agent to execute on (null = use alert's agent)
    
    @Column(name = "depends_on")
    private Long dependsOn; // ID of the step this depends on (null if no dependency)
    
    private String notes; // Additional notes or warnings
    
    // Constructors
    public EmergencyStep() {}
    
    public EmergencyStep(Long knowledgeId, int stepOrder, String description, String linuxCommand) {
        this.knowledgeId = knowledgeId;
        this.stepOrder = stepOrder;
        this.description = description;
        this.linuxCommand = linuxCommand;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getKnowledgeId() {
        return knowledgeId;
    }
    
    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }
    
    public int getStepOrder() {
        return stepOrder;
    }
    
    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLinuxCommand() {
        return linuxCommand;
    }
    
    public void setLinuxCommand(String linuxCommand) {
        this.linuxCommand = linuxCommand;
    }
    
    public Long getAgentId() {
        return agentId;
    }
    
    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
    
    public Long getDependsOn() {
        return dependsOn;
    }
    
    public void setDependsOn(Long dependsOn) {
        this.dependsOn = dependsOn;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}

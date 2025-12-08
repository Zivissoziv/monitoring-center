package com.example.monitoring.emergency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "emergency_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private String agentId; // ID of the agent to execute on (null = use alert's agent)
    
    @Column(name = "depends_on")
    private Long dependsOn; // ID of the step this depends on (null if no dependency)
    
    private String notes; // Additional notes or warnings
    
    public EmergencyStep(Long knowledgeId, int stepOrder, String description, String linuxCommand) {
        this.knowledgeId = knowledgeId;
        this.stepOrder = stepOrder;
        this.description = description;
        this.linuxCommand = linuxCommand;
    }
}

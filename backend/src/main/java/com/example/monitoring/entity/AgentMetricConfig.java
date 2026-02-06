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

/**
 * Configuration for which metrics are enabled for which agents
 */
@Entity
@Table(name = "agent_metric_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentMetricConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "agent_id", nullable = false)
    private String agentId;
    
    @Column(name = "metric_name", nullable = false)
    private String metricName;
    
    @Column(name = "enabled")
    private boolean enabled = true;
    
    @Column(name = "custom_interval")
    private Integer customInterval; // Override default interval, null = use definition default
    
    public AgentMetricConfig(String agentId, String metricName) {
        this.agentId = agentId;
        this.metricName = metricName;
    }
}

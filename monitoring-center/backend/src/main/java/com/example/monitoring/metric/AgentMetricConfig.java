package com.example.monitoring.metric;

import javax.persistence.*;

/**
 * Configuration for which metrics are enabled for which agents
 */
@Entity
@Table(name = "agent_metric_configs")
public class AgentMetricConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "agent_id", nullable = false)
    private Long agentId;
    
    @Column(name = "metric_name", nullable = false)
    private String metricName;
    
    @Column(name = "enabled")
    private boolean enabled = true;
    
    @Column(name = "custom_interval")
    private Integer customInterval; // Override default interval, null = use definition default
    
    @Column(name = "last_collection_time")
    private Long lastCollectionTime;
    
    // Constructors
    public AgentMetricConfig() {}
    
    public AgentMetricConfig(Long agentId, String metricName) {
        this.agentId = agentId;
        this.metricName = metricName;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAgentId() {
        return agentId;
    }
    
    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
    
    public String getMetricName() {
        return metricName;
    }
    
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Integer getCustomInterval() {
        return customInterval;
    }
    
    public void setCustomInterval(Integer customInterval) {
        this.customInterval = customInterval;
    }
    
    public Long getLastCollectionTime() {
        return lastCollectionTime;
    }
    
    public void setLastCollectionTime(Long lastCollectionTime) {
        this.lastCollectionTime = lastCollectionTime;
    }
}

package com.example.monitoring.service;

import com.example.monitoring.entity.AgentMetricConfig;
import com.example.monitoring.entity.MetricDefinition;
import com.example.monitoring.repository.AgentMetricConfigRepository;
import com.example.monitoring.repository.MetricDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AgentMetricConfigService {
    
    @Autowired
    private AgentMetricConfigRepository configRepository;
    
    @Autowired
    private MetricDefinitionRepository definitionRepository;
    
    public List<AgentMetricConfig> getAllConfigs() {
        return configRepository.findAll();
    }
    
    public List<AgentMetricConfig> getConfigsByAgent(String agentId) {
        return configRepository.findByAgentId(agentId);
    }
    
    public List<AgentMetricConfig> getEnabledConfigsByAgent(String agentId) {
        return configRepository.findByAgentIdAndEnabled(agentId, true);
    }
    
    public Optional<AgentMetricConfig> getConfig(String agentId, String metricName) {
        return configRepository.findByAgentIdAndMetricName(agentId, metricName);
    }
    
    public AgentMetricConfig createOrUpdateConfig(AgentMetricConfig config) {
        Optional<AgentMetricConfig> existing = configRepository
                .findByAgentIdAndMetricName(config.getAgentId(), config.getMetricName());
        
        if (existing.isPresent()) {
            AgentMetricConfig existingConfig = existing.get();
            existingConfig.setEnabled(config.isEnabled());
            existingConfig.setCustomInterval(config.getCustomInterval());
            return configRepository.save(existingConfig);
        } else {
            return configRepository.save(config);
        }
    }
    
    public void deleteConfig(Long id) {
        configRepository.deleteById(id);
    }
    
    /**
     * Initialize default metric configs for an agent
     * Creates configs for all enabled metric definitions
     */
    public void initializeDefaultConfigsForAgent(String agentId) {
        List<MetricDefinition> enabledDefinitions = definitionRepository.findByEnabled(true);
        
        for (MetricDefinition definition : enabledDefinitions) {
            Optional<AgentMetricConfig> existing = configRepository
                    .findByAgentIdAndMetricName(agentId, definition.getMetricName());
            
            if (!existing.isPresent()) {
                AgentMetricConfig config = new AgentMetricConfig(agentId, definition.getMetricName());
                config.setEnabled(true);
                configRepository.save(config);
            }
        }
    }
    
    /**
     * Get effective collection interval for a metric on an agent
     */
    public int getEffectiveInterval(String agentId, String metricName) {
        Optional<AgentMetricConfig> config = configRepository
                .findByAgentIdAndMetricName(agentId, metricName);
        
        if (config.isPresent() && config.get().getCustomInterval() != null) {
            return config.get().getCustomInterval();
        }
        
        // Fall back to definition default
        return definitionRepository.findByMetricName(metricName)
                .map(MetricDefinition::getCollectionInterval)
                .orElse(30); // Ultimate fallback
    }
}

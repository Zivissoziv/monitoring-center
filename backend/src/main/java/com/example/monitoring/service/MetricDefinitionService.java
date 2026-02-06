package com.example.monitoring.service;

import com.example.monitoring.entity.Agent;
import com.example.monitoring.entity.AgentMetricConfig;
import com.example.monitoring.entity.MetricDefinition;
import com.example.monitoring.repository.AgentMetricConfigRepository;
import com.example.monitoring.repository.AgentRepository;
import com.example.monitoring.repository.MetricDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MetricDefinitionService {
    
    @Autowired
    private MetricDefinitionRepository metricDefinitionRepository;
    
    @Autowired
    private AgentMetricConfigRepository agentMetricConfigRepository;
    
    @Autowired
    private AgentRepository agentRepository;
    
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    
    public List<MetricDefinition> getAllDefinitions() {
        return metricDefinitionRepository.findAll();
    }
    
    public List<MetricDefinition> getEnabledDefinitions() {
        return metricDefinitionRepository.findByEnabled(true);
    }
    
    public Optional<MetricDefinition> getDefinitionById(Long id) {
        return metricDefinitionRepository.findById(id);
    }
    
    public Optional<MetricDefinition> getDefinitionByName(String metricName) {
        return metricDefinitionRepository.findByMetricName(metricName);
    }
    
    public MetricDefinition createDefinition(MetricDefinition definition) {
        definition.setCreatedAt(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        MetricDefinition saved = metricDefinitionRepository.save(definition);
        
        // Auto-create configs for all existing agents if metric is enabled
        if (saved.getEnabled()) {
            initializeConfigsForAllAgents(saved.getMetricName());
        }
        
        return saved;
    }
    
    public MetricDefinition updateDefinition(Long id, MetricDefinition definitionDetails) {
        MetricDefinition definition = metricDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Metric definition not found with id: " + id));
        
        boolean wasDisabled = !definition.getEnabled();
        
        definition.setMetricName(definitionDetails.getMetricName());
        definition.setDisplayName(definitionDetails.getDisplayName());
        definition.setDescription(definitionDetails.getDescription());
        definition.setCollectionCommand(definitionDetails.getCollectionCommand());
        definition.setCollectionInterval(definitionDetails.getCollectionInterval());
        definition.setProcessingRule(definitionDetails.getProcessingRule());
        definition.setUnit(definitionDetails.getUnit());
        definition.setEnabled(definitionDetails.getEnabled());
        definition.setUpdatedAt(LocalDateTime.now());
        
        MetricDefinition saved = metricDefinitionRepository.save(definition);
        
        // If metric was just enabled, create configs for agents that don't have it
        if (wasDisabled && saved.getEnabled()) {
            initializeConfigsForAllAgents(saved.getMetricName());
        }
        
        return saved;
    }
    
    public void deleteDefinition(Long id) {
        metricDefinitionRepository.deleteById(id);
    }
    
    /**
     * Process raw command output using the processing rule
     * @param rawOutput The raw command output
     * @param processingRule JavaScript expression to process the output
     * @return Processed metric value
     */
    public double processMetricValue(String rawOutput, String processingRule) {
        log.debug("Processing output: [{}], rule: {}", rawOutput, processingRule);
        
        // Check for empty output
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            throw new RuntimeException("Command output is empty");
        }
        
        if (processingRule == null || processingRule.trim().isEmpty()) {
            // If no processing rule, try to parse as double
            try {
                double value = Double.parseDouble(rawOutput.trim());
                log.debug("Parsed value: {}", value);
                return value;
            } catch (NumberFormatException e) {
                throw new RuntimeException("Failed to parse metric value: [" + rawOutput + "]", e);
            }
        }
        
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
            engine.put("output", rawOutput);
            engine.put("value", rawOutput.trim());
            
            Object result = engine.eval(processingRule);
            log.debug("Script result: {}", result);
            
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            } else {
                return Double.parseDouble(result.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process metric value with rule: " + e.getMessage() + 
                                     ", output: [" + rawOutput + "]", e);
        }
    }
    
    /**
     * Initialize configs for all agents for a specific metric
     */
    private void initializeConfigsForAllAgents(String metricName) {
        List<Agent> agents = agentRepository.findAll();
        
        for (Agent agent : agents) {
            // Check if config already exists
            java.util.Optional<AgentMetricConfig> existing =
                agentMetricConfigRepository.findByAgentIdAndMetricName(agent.getId(), metricName);
            
            if (!existing.isPresent()) {
                // Create new config
                AgentMetricConfig config =
                    new AgentMetricConfig(agent.getId(), metricName);
                config.setEnabled(true);
                agentMetricConfigRepository.save(config);
            }
        }
    }
}

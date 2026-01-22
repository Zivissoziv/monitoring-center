package com.example.monitoring.service;

import com.example.monitoring.dto.MetricCollectionConfig;
import com.example.monitoring.entity.Agent;
import com.example.monitoring.entity.AgentMetricConfig;
import com.example.monitoring.entity.MetricDefinition;
import com.example.monitoring.repository.AgentMetricConfigRepository;
import com.example.monitoring.repository.AgentRepository;
import com.example.monitoring.repository.MetricDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for pushing metric collection configurations to agents
 */
@Slf4j
@Service
public class AgentConfigService {
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private MetricDefinitionRepository metricDefinitionRepository;
    
    @Autowired
    private AgentMetricConfigRepository agentMetricConfigRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Push metric collection configurations to a specific agent
     */
    public void pushConfigToAgent(String agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + agentId));
        
        log.info("Pushing metric configurations to agent: {} ({}:{})", 
                agent.getName(), agent.getIp(), agent.getPort());
        
        try {
            // Get all metric definitions
            List<MetricDefinition> allDefinitions = metricDefinitionRepository.findAll();
            
            // Get agent-specific configs
            List<AgentMetricConfig> agentConfigs = agentMetricConfigRepository.findByAgentId(agentId);
            
            // Build configuration list
            List<MetricCollectionConfig> configs = new ArrayList<>();
            
            for (MetricDefinition definition : allDefinitions) {
                // Check if agent has specific config for this metric
                AgentMetricConfig agentConfig = agentConfigs.stream()
                        .filter(c -> c.getMetricName().equals(definition.getMetricName()))
                        .findFirst()
                        .orElse(null);
                
                // Determine if metric should be enabled for this agent
                boolean enabled = definition.getEnabled();
                Integer customInterval = null;
                
                if (agentConfig != null) {
                    enabled = agentConfig.isEnabled();
                    customInterval = agentConfig.getCustomInterval();
                }
                
                if (!enabled) {
                    continue;  // Skip disabled metrics
                }
                
                // Create config
                MetricCollectionConfig config = new MetricCollectionConfig();
                config.setMetricName(definition.getMetricName());
                config.setDisplayName(definition.getDisplayName());
                config.setMetricType(definition.getMetricType().name());
                config.setCollectionCommand(definition.getCollectionCommand());
                config.setCollectionInterval(customInterval != null ? customInterval : definition.getCollectionInterval());
                config.setProcessingRule(definition.getProcessingRule());
                config.setUnit(definition.getUnit());
                config.setEnabled(enabled);
                
                configs.add(config);
            }
            
            // Push to agent
            String agentUrl = String.format("http://%s:%d/api/config/metrics", agent.getIp(), agent.getPort());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<MetricCollectionConfig>> request = new HttpEntity<>(configs, headers);
            
            restTemplate.postForObject(agentUrl, request, String.class);
            
            log.info("Successfully pushed {} metric configurations to agent: {}", configs.size(), agent.getName());
        } catch (Exception e) {
            log.error("Failed to push configurations to agent {}: {}", agentId, e.getMessage(), e);
            throw new RuntimeException("Failed to push configurations to agent: " + e.getMessage());
        }
    }
    
    /**
     * Push configurations to all active agents
     */
    public void pushConfigToAllAgents() {
        List<Agent> agents = agentRepository.findAll();
        
        log.info("Pushing configurations to all agents, count: {}", agents.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Agent agent : agents) {
            try {
                pushConfigToAgent(agent.getId());
                successCount++;
            } catch (Exception e) {
                log.error("Failed to push config to agent {}: {}", agent.getId(), e.getMessage());
                failCount++;
            }
        }
        
        log.info("Configuration push completed: {} succeeded, {} failed", successCount, failCount);
    }
}

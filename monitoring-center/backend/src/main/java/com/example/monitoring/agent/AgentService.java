package com.example.monitoring.agent;

import com.example.monitoring.alert.AlertService;
import com.example.monitoring.event.MetricCollectedEvent;
import com.example.monitoring.metric.Metric;
import com.example.monitoring.metric.MetricRepository;
import com.example.monitoring.metric.MetricDefinition;
import com.example.monitoring.metric.MetricDefinitionService;
import com.example.monitoring.metric.AgentMetricConfig;
import com.example.monitoring.metric.AgentMetricConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AgentService {
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private MetricDefinitionService metricDefinitionService;
    
    @Autowired
    private AgentMetricConfigService agentMetricConfigService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }
    
    public Optional<Agent> getAgentById(String id) {
        return agentRepository.findById(id);
    }
    
    public Agent createAgent(Agent agent) {
        // Set initial status
        agent.setStatus("INACTIVE");
        Agent savedAgent = agentRepository.save(agent);
        
        // Initialize default metric configs for this agent
        agentMetricConfigService.initializeDefaultConfigsForAgent(savedAgent.getId());
        
        // Try to connect to agent to verify it's reachable
        checkAgentHealth(savedAgent);
        
        return savedAgent;
    }
    
    public Agent updateAgent(String id, Agent agentDetails) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
        
        agent.setName(agentDetails.getName());
        agent.setIp(agentDetails.getIp());
        agent.setPort(agentDetails.getPort());
        agent.setStatus(agentDetails.getStatus());
        
        return agentRepository.save(agent);
    }
    
    public void deleteAgent(String id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
        
        agentRepository.delete(agent);
    }
    
    /**
     * Check agent health status
     */
    public void checkAgentHealth(Agent agent) {
        try {
            String url = "http://" + agent.getIp() + ":" + agent.getPort() + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                agent.setStatus("ACTIVE");
            } else {
                agent.setStatus("DISCONNECTED");
            }
        } catch (Exception e) {
            log.error("Failed to check agent health for {}: {}", agent.getId(), e.getMessage(), e);
            agent.setStatus("DISCONNECTED");
        }
        agentRepository.save(agent);
    }
    
    /**
     * Collect metrics from agent using custom metric definitions
     */
    public void collectMetricsFromAgent(String agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + agentId));
        
        // Get enabled metric configs for this agent
        List<AgentMetricConfig> configs = agentMetricConfigService.getEnabledConfigsByAgent(agentId);
        
        for (AgentMetricConfig config : configs) {
            try {
                // Get metric definition
                MetricDefinition definition = metricDefinitionService.getDefinitionByName(config.getMetricName())
                        .orElse(null);
                
                if (definition == null || !definition.getEnabled()) {
                    continue;
                }
                
                // Check if it's time to collect (based on interval)
                long now = System.currentTimeMillis();
                int interval = agentMetricConfigService.getEffectiveInterval(agentId, config.getMetricName());
                
                if (config.getLastCollectionTime() != null) {
                    long timeSinceLastCollection = (now - config.getLastCollectionTime()) / 1000;
                    if (timeSinceLastCollection < interval) {
                        continue; // Skip, not time yet
                    }
                }
                
                // Execute collection command on agent
                Map<String, Object> result = executeCommandOnAgent(agentId, definition.getCollectionCommand());
                
                // Check for errors
                if (result.containsKey("error") && result.get("error") != null && !result.get("error").toString().isEmpty()) {
                    log.error("Failed to collect metric {} from agent {}: {}", 
                            config.getMetricName(), agentId, result.get("error"));
                    continue;
                }
                
                // Check exit code
                if (result.containsKey("exitCode") && !Integer.valueOf(0).equals(result.get("exitCode"))) {
                    log.error("Failed to collect metric {} from agent {}: Command exited with code {}, stderr: {}",
                            config.getMetricName(), agentId, result.get("exitCode"), result.get("error"));
                    continue;
                }
                
                // Extract output from result
                String output = result.containsKey("output") ? result.get("output").toString() : "";
                
                // Process the output using the processing rule
                double value = metricDefinitionService.processMetricValue(output, definition.getProcessingRule());
                
                // Save metric
                Metric metric = new Metric();
                metric.setAgentId(agentId);
                metric.setMetricType(config.getMetricName());
                metric.setValue(value);
                metric.setTimestamp(now);
                Metric savedMetric = metricRepository.save(metric);
                
                // Publish event for real-time alert processing
                eventPublisher.publishEvent(new MetricCollectedEvent(savedMetric));
                
                // Update last collection time
                agentMetricConfigService.updateLastCollectionTime(agentId, config.getMetricName());
                
                // Update agent status to ACTIVE
                agent.setStatus("ACTIVE");
                agentRepository.save(agent);
                
            } catch (Exception e) {
                log.error("Error collecting metric {} from agent {}: {}", 
                        config.getMetricName(), agentId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Collect metrics from all active agents
     */
    public void collectMetricsFromAllAgents() {
        List<Agent> agents = agentRepository.findAll();
        for (Agent agent : agents) {
            if (!"INACTIVE".equals(agent.getStatus())) {
                collectMetricsFromAgent(agent.getId());
            }
        }
    }
    
    /**
     * Execute command on agent
     */
    public Map<String, Object> executeCommandOnAgent(String agentId, String command) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + agentId));
        
        try {
            String url = "http://" + agent.getIp() + ":" + agent.getPort() + "/execute";
            
            // Prepare request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("command", command);
            
            // Send POST request to agent
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to execute command on agent: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to execute command on agent {}: {}", agentId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to execute command: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return errorResponse;
        }
    }
}
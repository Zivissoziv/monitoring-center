package com.example.monitoring.agent;

import com.example.monitoring.alert.AlertService;
import com.example.monitoring.metric.Metric;
import com.example.monitoring.metric.MetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AgentService {
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private AlertService alertService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }
    
    public Optional<Agent> getAgentById(Long id) {
        return agentRepository.findById(id);
    }
    
    public Agent createAgent(Agent agent) {
        // Set initial status
        agent.setStatus("INACTIVE");
        Agent savedAgent = agentRepository.save(agent);
        
        // Try to connect to agent to verify it's reachable
        checkAgentHealth(savedAgent);
        
        return savedAgent;
    }
    
    public Agent updateAgent(Long id, Agent agentDetails) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
        
        agent.setName(agentDetails.getName());
        agent.setIp(agentDetails.getIp());
        agent.setPort(agentDetails.getPort());
        agent.setStatus(agentDetails.getStatus());
        
        return agentRepository.save(agent);
    }
    
    public void deleteAgent(Long id) {
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
            agent.setStatus("DISCONNECTED");
        }
        agentRepository.save(agent);
    }
    
    /**
     * Collect metrics from agent
     */
    public void collectMetricsFromAgent(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + agentId));
        
        try {
            String url = "http://" + agent.getIp() + ":" + agent.getPort() + "/metrics";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> metricsData = response.getBody();
                
                // Save CPU metric
                if (metricsData.containsKey("cpu")) {
                    Metric cpuMetric = new Metric();
                    cpuMetric.setAgentId(agentId);
                    cpuMetric.setMetricType("CPU");
                    cpuMetric.setValue(((Number) metricsData.get("cpu")).doubleValue());
                    cpuMetric.setTimestamp(System.currentTimeMillis());
                    Metric savedCpuMetric = metricRepository.save(cpuMetric);
                    // Check if any alerts should be triggered
                    alertService.checkAlert(savedCpuMetric);
                }
                
                // Save Memory metric
                if (metricsData.containsKey("memory")) {
                    Metric memoryMetric = new Metric();
                    memoryMetric.setAgentId(agentId);
                    memoryMetric.setMetricType("MEMORY");
                    memoryMetric.setValue(((Number) metricsData.get("memory")).doubleValue());
                    memoryMetric.setTimestamp(System.currentTimeMillis());
                    Metric savedMemoryMetric = metricRepository.save(memoryMetric);
                    // Check if any alerts should be triggered
                    alertService.checkAlert(savedMemoryMetric);
                }
                
                // Update agent status to ACTIVE
                agent.setStatus("ACTIVE");
                agentRepository.save(agent);
            }
        } catch (Exception e) {
            agent.setStatus("DISCONNECTED");
            agentRepository.save(agent);
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
    public Map<String, Object> executeCommandOnAgent(Long agentId, String command) {
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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to execute command: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return errorResponse;
        }
    }
}
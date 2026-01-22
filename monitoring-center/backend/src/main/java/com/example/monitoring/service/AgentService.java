package com.example.monitoring.service;

import com.example.monitoring.entity.Agent;
import com.example.monitoring.enums.AgentStatus;
import com.example.monitoring.repository.AgentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private AgentMetricConfigService agentMetricConfigService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }
    
    public Optional<Agent> getAgentById(String id) {
        return agentRepository.findById(id);
    }
    
    public Agent createAgent(Agent agent) {
        // Set initial status
        agent.setStatus(AgentStatus.INACTIVE);
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
                agent.setStatus(AgentStatus.ACTIVE);
            } else {
                agent.setStatus(AgentStatus.DISCONNECTED);
            }
        } catch (Exception e) {
            log.error("Failed to check agent health for {}: {}", agent.getId(), e.getMessage(), e);
            agent.setStatus(AgentStatus.DISCONNECTED);
        }
        agentRepository.save(agent);
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
package com.example.monitoring.agent;

import com.example.monitoring.metric.Metric;
import com.example.monitoring.metric.MetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AgentService {
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private MetricRepository metricRepository;
    
    private final RestTemplate restTemplate;
    
    public AgentService() {
        this.restTemplate = new RestTemplate();
        // Set connection timeout to 5 seconds and read timeout to 5 seconds
        this.restTemplate.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory());
        ((org.springframework.http.client.SimpleClientHttpRequestFactory) this.restTemplate.getRequestFactory())
                .setConnectTimeout(5000);
        ((org.springframework.http.client.SimpleClientHttpRequestFactory) this.restTemplate.getRequestFactory())
                .setReadTimeout(5000);
    }
    
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
            System.out.println("Checking agent health: " + url);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                agent.setStatus("ACTIVE");
                System.out.println("Agent " + agent.getName() + " is ACTIVE");
            } else {
                agent.setStatus("DISCONNECTED");
                System.err.println("Agent " + agent.getName() + " returned status: " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            agent.setStatus("DISCONNECTED");
            System.err.println("Cannot connect to agent " + agent.getName() + " at " + agent.getIp() + ":" + agent.getPort());
            System.err.println("Please ensure:");
            System.err.println("  1. Agent is running on " + agent.getIp() + ":" + agent.getPort());
            System.err.println("  2. Network connectivity is available");
            System.err.println("  3. Firewall allows connection to port " + agent.getPort());
        } catch (Exception e) {
            agent.setStatus("DISCONNECTED");
            System.err.println("Failed to connect to agent " + agent.getName() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
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
            System.out.println("Collecting metrics from: " + url);
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
                    metricRepository.save(cpuMetric);
                }
                
                // Save Memory metric
                if (metricsData.containsKey("memory")) {
                    Metric memoryMetric = new Metric();
                    memoryMetric.setAgentId(agentId);
                    memoryMetric.setMetricType("MEMORY");
                    memoryMetric.setValue(((Number) metricsData.get("memory")).doubleValue());
                    memoryMetric.setTimestamp(System.currentTimeMillis());
                    metricRepository.save(memoryMetric);
                }
                
                // Update agent status to ACTIVE
                agent.setStatus("ACTIVE");
                agentRepository.save(agent);
                
                System.out.println("Successfully collected metrics from agent: " + agent.getName());
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            agent.setStatus("DISCONNECTED");
            agentRepository.save(agent);
            System.err.println("Cannot connect to agent " + agent.getName() + " at " + agent.getIp() + ":" + agent.getPort());
            System.err.println("Connection error: " + e.getCause().getClass().getSimpleName());
        } catch (Exception e) {
            agent.setStatus("DISCONNECTED");
            agentRepository.save(agent);
            System.err.println("Failed to collect metrics from agent " + agent.getName() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
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
}
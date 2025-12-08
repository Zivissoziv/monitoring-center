package com.example.monitoring.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentController {
    
    @Autowired
    private AgentService agentService;
    
    @GetMapping
    public List<Agent> getAllAgents() {
        return agentService.getAllAgents();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgentById(@PathVariable String id) {
        return agentService.getAgentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public Agent createAgent(@RequestBody Agent agent) {
        log.info("POST /api/agents - Creating agent: {}", agent.getName());
        Agent result = agentService.createAgent(agent);
        log.info("POST /api/agents - Success");
        return result;
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Agent> updateAgent(@PathVariable String id, @RequestBody Agent agentDetails) {
        try {
            Agent updatedAgent = agentService.updateAgent(id, agentDetails);
            return ResponseEntity.ok(updatedAgent);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(@PathVariable String id) {
        try {
            agentService.deleteAgent(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/collect")
    public ResponseEntity<String> collectMetrics(@PathVariable String id) {
        try {
            agentService.collectMetricsFromAgent(id);
            return ResponseEntity.ok("Metrics collection started for agent " + id);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/collect-all")
    public ResponseEntity<String> collectAllMetrics() {
        agentService.collectMetricsFromAllAgents();
        return ResponseEntity.ok("Metrics collection started for all agents");
    }
    
    @PostMapping("/{id}/health")
    public ResponseEntity<Agent> checkHealth(@PathVariable String id) {
        try {
            Agent agent = agentService.getAgentById(id)
                    .orElseThrow(() -> new RuntimeException("Agent not found"));
            agentService.checkAgentHealth(agent);
            return ResponseEntity.ok(agent);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // New endpoint for executing commands on agents
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeCommand(@PathVariable String id, @RequestBody Map<String, String> commandRequest) {
        try {
            Map<String, Object> result = agentService.executeCommandOnAgent(id, commandRequest.get("command"));
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
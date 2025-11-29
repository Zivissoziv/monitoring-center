package com.example.monitoring.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Agent> getAgentById(@PathVariable Long id) {
        return agentService.getAgentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public Agent createAgent(@RequestBody Agent agent) {
        return agentService.createAgent(agent);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Agent> updateAgent(@PathVariable Long id, @RequestBody Agent agentDetails) {
        try {
            Agent updatedAgent = agentService.updateAgent(id, agentDetails);
            return ResponseEntity.ok(updatedAgent);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(@PathVariable Long id) {
        try {
            agentService.deleteAgent(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/collect")
    public ResponseEntity<String> collectMetrics(@PathVariable Long id) {
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
    public ResponseEntity<Agent> checkHealth(@PathVariable Long id) {
        try {
            Agent agent = agentService.getAgentById(id)
                    .orElseThrow(() -> new RuntimeException("Agent not found"));
            agentService.checkAgentHealth(agent);
            return ResponseEntity.ok(agent);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
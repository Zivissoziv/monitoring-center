package com.example.monitoring.controller;

import com.example.monitoring.entity.AgentMetricConfig;
import com.example.monitoring.service.AgentMetricConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent-metric-configs")
@CrossOrigin(origins = "*")
public class AgentMetricConfigController {
    
    @Autowired
    private AgentMetricConfigService configService;
    
    @GetMapping
    public List<AgentMetricConfig> getAllConfigs() {
        return configService.getAllConfigs();
    }
    
    @GetMapping("/agent/{agentId}")
    public List<AgentMetricConfig> getConfigsByAgent(@PathVariable String agentId) {
        return configService.getConfigsByAgent(agentId);
    }
    
    @GetMapping("/agent/{agentId}/enabled")
    public List<AgentMetricConfig> getEnabledConfigsByAgent(@PathVariable String agentId) {
        return configService.getEnabledConfigsByAgent(agentId);
    }
    
    @PostMapping
    public AgentMetricConfig createOrUpdateConfig(@RequestBody AgentMetricConfig config) {
        return configService.createOrUpdateConfig(config);
    }
    
    @PostMapping("/agent/{agentId}/initialize")
    public ResponseEntity<String> initializeDefaultConfigs(@PathVariable String agentId) {
        configService.initializeDefaultConfigsForAgent(agentId);
        return ResponseEntity.ok("Default metric configs initialized for agent " + agentId);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        configService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }
}

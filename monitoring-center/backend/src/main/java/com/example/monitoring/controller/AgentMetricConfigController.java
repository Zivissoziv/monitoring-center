package com.example.monitoring.controller;

import com.example.monitoring.entity.AgentMetricConfig;
import com.example.monitoring.service.AgentMetricConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent-metric-configs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AgentMetricConfigController {
    
    private final AgentMetricConfigService configService;
    
    @GetMapping
    public ResponseEntity<List<AgentMetricConfig>> getAllConfigs() {
        return ResponseEntity.ok(configService.getAllConfigs());
    }
    
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<AgentMetricConfig>> getConfigsByAgent(@PathVariable String agentId) {
        return ResponseEntity.ok(configService.getConfigsByAgent(agentId));
    }
    
    @GetMapping("/agent/{agentId}/enabled")
    public ResponseEntity<List<AgentMetricConfig>> getEnabledConfigsByAgent(@PathVariable String agentId) {
        return ResponseEntity.ok(configService.getEnabledConfigsByAgent(agentId));
    }
    
    @PostMapping
    public ResponseEntity<AgentMetricConfig> createOrUpdateConfig(@RequestBody AgentMetricConfig config) {
        AgentMetricConfig saved = configService.createOrUpdateConfig(config);
        return ResponseEntity.status(config.getId() == null ? HttpStatus.CREATED : HttpStatus.OK).body(saved);
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

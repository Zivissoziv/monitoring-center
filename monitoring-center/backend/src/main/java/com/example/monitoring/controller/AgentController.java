package com.example.monitoring.controller;

import com.example.monitoring.entity.Agent;
import com.example.monitoring.service.AgentConfigService;
import com.example.monitoring.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AgentController {
    
    private final AgentService agentService;
    private final AgentConfigService agentConfigService;
    
    @GetMapping
    public ResponseEntity<List<Agent>> getAllAgents() {
        return ResponseEntity.ok(agentService.getAllAgents());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgentById(@PathVariable String id) {
        return agentService.getAgentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Agent> createAgent(@RequestBody Agent agent) {
        log.info("POST /api/agents - Creating agent: {}", agent.getName());
        Agent result = agentService.createAgent(agent);
        log.info("POST /api/agents - Success, id: {}", result.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Agent> updateAgent(@PathVariable String id, @RequestBody Agent agentDetails) {
        try {
            Agent updatedAgent = agentService.updateAgent(id, agentDetails);
            return ResponseEntity.ok(updatedAgent);
        } catch (RuntimeException e) {
            log.error("Failed to update agent {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(@PathVariable String id) {
        try {
            agentService.deleteAgent(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Failed to delete agent {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/health")
    public ResponseEntity<Agent> checkHealth(@PathVariable String id) {
        try {
            Agent agent = agentService.getAgentById(id)
                    .orElseThrow(() -> new RuntimeException("Agent not found"));
            agentService.checkAgentHealth(agent);
            return ResponseEntity.ok(agent);
        } catch (RuntimeException e) {
            log.error("Failed to check health for agent {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 测试连接
     */
    @PostMapping("/connection-test")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, Object> request) {
        try {
            String ip = (String) request.get("ip");
            Integer port = (Integer) request.get("port");
            
            if (ip == null || port == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "IP and port are required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Simple telnet test - try to connect to the port
            boolean canConnect = false;
            String message = "";
            
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(ip, port), 3000); // 3 second timeout
                canConnect = true;
                message = "Connection successful";
            } catch (java.net.SocketTimeoutException e) {
                message = "Connection timeout";
                log.debug("Connection timeout to {}:{}", ip, port);
            } catch (java.net.ConnectException e) {
                message = "Connection refused";
                log.debug("Connection refused to {}:{}", ip, port);
            } catch (Exception e) {
                message = "Connection failed: " + e.getMessage();
                log.error("Connection test failed to {}:{}: {}", ip, port, e.getMessage(), e);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", canConnect);
            result.put("status", canConnect ? "REACHABLE" : "UNREACHABLE");
            result.put("message", message);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Test connection failed: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Test failed: " + e.getMessage());
            return ResponseEntity.ok(error);
        }
    }
    
    /**
     * 执行命令
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeCommand(@PathVariable String id, @RequestBody Map<String, String> commandRequest) {
        try {
            Map<String, Object> result = agentService.executeCommandOnAgent(id, commandRequest.get("command"));
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Failed to execute command on agent {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 推送配置到指定Agent
     */
    @PostMapping("/{id}/config")
    public ResponseEntity<String> pushConfigToAgent(@PathVariable String id) {
        try {
            log.info("Pushing config to agent: {}", id);
            agentConfigService.pushConfigToAgent(id);
            return ResponseEntity.ok("Configuration pushed successfully");
        } catch (Exception e) {
            log.error("Failed to push config to agent {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to push configuration: " + e.getMessage());
        }
    }
    
    /**
     * 推送配置到所有Agent
     */
    @PostMapping("/config/batch-push")
    public ResponseEntity<String> pushConfigToAllAgents() {
        try {
            log.info("Pushing config to all agents");
            agentConfigService.pushConfigToAllAgents();
            return ResponseEntity.ok("Configuration pushed to all agents");
        } catch (Exception e) {
            log.error("Failed to push config to all agents: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to push configuration: " + e.getMessage());
        }
    }
}
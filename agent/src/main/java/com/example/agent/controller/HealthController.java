package com.example.agent.controller;

import com.example.agent.config.AgentProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private AgentProperties agentProperties;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("agentName", agentProperties.getName() != null ? agentProperties.getName() : getHostname());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
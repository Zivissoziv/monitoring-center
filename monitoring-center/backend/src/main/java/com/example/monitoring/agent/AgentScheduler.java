package com.example.monitoring.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentScheduler {
    
    @Autowired
    private AgentService agentService;
    
    /**
     * Scheduled task to collect metrics from all agents every 30 seconds
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void collectMetricsFromAllAgents() {
        System.out.println("Starting scheduled metrics collection from all agents...");
        agentService.collectMetricsFromAllAgents();
    }
}

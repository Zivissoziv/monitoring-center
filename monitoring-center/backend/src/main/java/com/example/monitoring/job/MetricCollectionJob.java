package com.example.monitoring.job;

import com.example.monitoring.agent.Agent;
import com.example.monitoring.agent.AgentRepository;
import com.example.monitoring.agent.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job for collecting metrics from all agents
 * Runs every 5 seconds to check if any metrics need collection
 * Actual collection interval is determined by metric definition configuration
 */
@Slf4j
@Component
public class MetricCollectionJob implements Job {
    
    @Autowired
    private AgentService agentService;
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting metric collection check");
        
        try {
            // Get all agents
            List<Agent> agents = agentRepository.findAll();
            
            log.debug("Checking {} agents for metric collection", agents.size());
            
            // Check and collect metrics from each agent
            // AgentService.collectMetricsFromAgent will check intervals internally
            for (Agent agent : agents) {
                try {
                    log.debug("Checking metrics for agent: {} ({})", agent.getName(), agent.getId());
                    agentService.collectMetricsFromAgent(agent.getId());
                } catch (Exception e) {
                    log.error("Error collecting metrics from agent {}: {}", agent.getId(), e.getMessage());
                }
            }
            
            log.debug("Metric collection check completed");
        } catch (Exception e) {
            log.error("Error in metric collection job: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}

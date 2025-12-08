package com.example.monitoring.job;

import com.example.monitoring.alert.Alert;
import com.example.monitoring.alert.AlertRepository;
import com.example.monitoring.alert.AlertRule;
import com.example.monitoring.alert.AlertRuleRepository;
import com.example.monitoring.metric.Metric;
import com.example.monitoring.metric.MetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job for checking alert conditions
 * Runs every 10 seconds
 */
@Slf4j
@Component
public class AlertCheckJob implements Job {
    
    @Autowired
    private AlertRuleRepository alertRuleRepository;
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting alert check job");
        
        try {
            // Get all enabled alert rules
            List<AlertRule> rules = alertRuleRepository.findByEnabled(true);
            
            log.debug("Checking {} enabled alert rules", rules.size());
            
            long now = System.currentTimeMillis();
            long checkWindow = 60000; // Check metrics from last 60 seconds
            
            for (AlertRule rule : rules) {
                try {
                    checkAlertRule(rule, now - checkWindow, now);
                } catch (Exception e) {
                    log.error("Error checking alert rule {}: {}", rule.getId(), e.getMessage());
                }
            }
            
            log.debug("Alert check job completed");
        } catch (Exception e) {
            log.error("Error in alert check job: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
    
    private void checkAlertRule(AlertRule rule, long startTime, long endTime) {
        // Get recent metrics for this rule
        List<Metric> metrics;
        
        if (rule.getAgentId() != null) {
            // Rule applies to specific agent
            metrics = metricRepository.findByAgentIdAndMetricTypeAndTimestampBetween(
                    rule.getAgentId(), rule.getMetricType(), startTime, endTime);
        } else {
            // Rule applies to all agents
            metrics = metricRepository.findByMetricTypeAndTimestampBetween(
                    rule.getMetricType(), startTime, endTime);
        }
        
        // Track which agents have triggered alerts in this check cycle
        java.util.Set<String> triggeredAgents = new java.util.HashSet<>();
        
        // Check each metric against the rule
        for (Metric metric : metrics) {
            boolean triggered = false;
            
            switch (rule.getCondition()) {
                case "GT":
                case ">":
                    triggered = metric.getValue() > rule.getThreshold();
                    break;
                case "LT":
                case "<":
                    triggered = metric.getValue() < rule.getThreshold();
                    break;
                case "EQ":
                case "=":
                    triggered = Math.abs(metric.getValue() - rule.getThreshold()) < 0.001;
                    break;
            }
            
            if (triggered) {
                handleTriggeredAlert(rule, metric);
                triggeredAgents.add(metric.getAgentId());
            }
        }
        
        // Auto-resolve alerts that were not triggered in this cycle
        autoResolveAlertsForRule(rule, triggeredAgents, metrics);
    }
    
    private void handleTriggeredAlert(AlertRule rule, Metric metric) {
        // Only look for ACTIVE or ACKNOWLEDGED alerts (not RESOLVED)
        Alert existingAlert = alertRepository.findByAlertRuleIdAndAgentIdAndStatusIn(
                rule.getId(), 
                metric.getAgentId(), 
                java.util.Arrays.asList("ACTIVE", "ACKNOWLEDGED")
        ).orElse(null);
        
        if (existingAlert != null) {
            // Update existing active or acknowledged alert
            existingAlert.setLastTriggeredAt(System.currentTimeMillis());
            existingAlert.setTriggerCount(existingAlert.getTriggerCount() + 1);
            existingAlert.setTriggerValue(metric.getValue());
            alertRepository.save(existingAlert);
            
            log.debug("Updated existing {} alert for rule {} on agent {}", 
                    existingAlert.getStatus(), rule.getId(), metric.getAgentId());
        } else {
            // No active/acknowledged alert exists, create a new one
            // This includes the case where a previous alert was RESOLVED
            Alert newAlert = new Alert(
                    rule.getId(),
                    metric.getAgentId(),
                    rule.getName(),
                    rule.getMetricType(),
                    metric.getValue(),
                    rule.getThreshold(),
                    rule.getSeverity()
            );
            alertRepository.save(newAlert);
            
            log.info("Created new alert for rule {} on agent {}: {} {} {}", 
                    rule.getId(), metric.getAgentId(), 
                    metric.getValue(), rule.getCondition(), rule.getThreshold());
        }
    }
    
    /**
     * Auto-resolve alerts for this rule that were not triggered in the current cycle
     */
    private void autoResolveAlertsForRule(AlertRule rule, java.util.Set<String> triggeredAgents, List<Metric> recentMetrics) {
        // Find all active/acknowledged alerts for this rule
        List<Alert> alertsForRule = alertRepository.findByStatusIn(
                java.util.Arrays.asList("ACTIVE", "ACKNOWLEDGED"));
        
        for (Alert alert : alertsForRule) {
            // Only process alerts for this specific rule
            if (!alert.getAlertRuleId().equals(rule.getId())) {
                continue;
            }
            
            // Check if this alert was triggered in the current cycle
            if (triggeredAgents.contains(alert.getAgentId())) {
                // Alert is still being triggered, skip
                continue;
            }
            
            // Alert was not triggered in this cycle, check if we should auto-resolve
            // Check if there are recent metrics for this agent
            boolean hasRecentMetrics = recentMetrics.stream()
                    .anyMatch(m -> m.getAgentId().equals(alert.getAgentId()));
            
            if (hasRecentMetrics) {
                // Has recent metrics but they don't violate the condition - auto-resolve
                alert.setStatus("RESOLVED");
                alert.setResolveNote("Auto-resolved: condition no longer met");
                alert.setResolvedAt(System.currentTimeMillis());
                alertRepository.save(alert);
                
                log.info("Auto-resolved alert {} for agent {} - recent metrics no longer violate condition (rule: {})",
                        alert.getId(), alert.getAgentId(), rule.getName());
            } else {
                // No recent metrics for this agent - auto-resolve
                alert.setStatus("RESOLVED");
                alert.setResolveNote("Auto-resolved: no recent metrics");
                alert.setResolvedAt(System.currentTimeMillis());
                alertRepository.save(alert);
                
                log.info("Auto-resolved alert {} for agent {} - no recent metrics (rule: {})",
                        alert.getId(), alert.getAgentId(), rule.getName());
            }
        }
    }
}

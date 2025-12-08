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
        
        // Check each metric against the rule
        for (Metric metric : metrics) {
            boolean triggered = false;
            
            switch (rule.getCondition()) {
                case "GT":
                    triggered = metric.getValue() > rule.getThreshold();
                    break;
                case "LT":
                    triggered = metric.getValue() < rule.getThreshold();
                    break;
                case "EQ":
                    triggered = Math.abs(metric.getValue() - rule.getThreshold()) < 0.001;
                    break;
            }
            
            if (triggered) {
                handleTriggeredAlert(rule, metric);
            }
        }
    }
    
    private void handleTriggeredAlert(AlertRule rule, Metric metric) {
        // Check if alert already exists
        Alert existingAlert = alertRepository.findByAlertRuleIdAndAgentId(
                rule.getId(), metric.getAgentId()).orElse(null);
        
        if (existingAlert != null && "ACTIVE".equals(existingAlert.getStatus())) {
            // Update existing alert
            existingAlert.setLastTriggeredAt(System.currentTimeMillis());
            existingAlert.setTriggerCount(existingAlert.getTriggerCount() + 1);
            existingAlert.setTriggerValue(metric.getValue());
            alertRepository.save(existingAlert);
            
            log.debug("Updated existing alert for rule {} on agent {}", 
                    rule.getId(), metric.getAgentId());
        } else if (existingAlert == null) {
            // Create new alert
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
}

package com.example.monitoring.job;

import com.example.monitoring.entity.Alert;
import com.example.monitoring.enums.AlertStatus;
import com.example.monitoring.repository.AlertRepository;
import com.example.monitoring.entity.AlertRule;
import com.example.monitoring.repository.AlertRuleRepository;
import com.example.monitoring.entity.Metric;
import com.example.monitoring.entity.MetricDefinition;
import com.example.monitoring.repository.MetricDefinitionRepository;
import com.example.monitoring.repository.MetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

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
    
    @Autowired
    private MetricDefinitionRepository metricDefinitionRepository;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Starting alert check job");
        
        try {
            // Get all enabled alert rules
            List<AlertRule> rules = alertRuleRepository.findByEnabled(true);
            
            log.debug("Checking {} enabled alert rules", rules.size());
            
            long now = System.currentTimeMillis();
            long checkWindow = 60000; // Check metrics from last 60 seconds
            
            // Convert to LocalDateTime for repository queries
            LocalDateTime startTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(now - checkWindow), ZoneId.systemDefault());
            LocalDateTime endTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(now), ZoneId.systemDefault());
            
            for (AlertRule rule : rules) {
                try {
                    checkAlertRule(rule, startTime, endTime);
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
    
    private void checkAlertRule(AlertRule rule, LocalDateTime startTime, LocalDateTime endTime) {
        // Get metric definition to determine metric type
        Optional<MetricDefinition> metricDefOpt = metricDefinitionRepository.findByMetricName(rule.getMetricType());
        String metricType = metricDefOpt.map(def -> def.getMetricType().name()).orElse("NUMERIC");
        
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
            boolean triggered = evaluateCondition(metric, rule, metricType);
            
            if (triggered) {
                handleTriggeredAlert(rule, metric);
                triggeredAgents.add(metric.getAgentId());
            }
        }
        
        // Auto-resolve alerts that were not triggered in this cycle
        autoResolveAlertsForRule(rule, triggeredAgents, metrics);
    }
    
    /**
     * Evaluate alert condition based on metric type
     */
    private boolean evaluateCondition(Metric metric, AlertRule rule, String metricType) {
        switch (metricType) {
            case "NUMERIC":
                return evaluateNumericCondition(metric.getValue(), rule);
            case "BOOLEAN":
                return evaluateBooleanCondition(metric.getValue(), rule);
            case "STRING":
                return evaluateStringCondition(metric.getTextValue(), rule);
            default:
                log.warn("Unknown metric type: {}, defaulting to NUMERIC", metricType);
                return evaluateNumericCondition(metric.getValue(), rule);
        }
    }
    
    /**
     * Evaluate numeric condition (GT, LT, EQ, GTE, LTE)
     */
    private boolean evaluateNumericCondition(double value, AlertRule rule) {
        if (rule.getThreshold() == null) {
            log.warn("Numeric rule {} has no threshold", rule.getId());
            return false;
        }
        
        double threshold = rule.getThreshold();
        switch (rule.getCondition()) {
            case GT:
                return value > threshold;
            case LT:
                return value < threshold;
            case GTE:
                return value >= threshold;
            case LTE:
                return value <= threshold;
            case EQ:
            case EQUALS:
                return Math.abs(value - threshold) < 0.001;
            case NOT_EQUALS:
                return Math.abs(value - threshold) >= 0.001;
            default:
                log.warn("Unknown numeric condition: {}", rule.getCondition());
                return false;
        }
    }
    
    /**
     * Evaluate boolean condition (EQUALS, NOT_EQUALS)
     */
    private boolean evaluateBooleanCondition(double value, AlertRule rule) {
        if (rule.getThresholdText() == null) {
            log.warn("Boolean rule {} has no threshold text", rule.getId());
            return false;
        }
        
        boolean metricBool = value > 0.5; // 0 = false, 1 = true
        boolean expectedBool = "true".equalsIgnoreCase(rule.getThresholdText()) || "1".equals(rule.getThresholdText());
        
        switch (rule.getCondition()) {
            case EQUALS:
            case EQ:
                return metricBool == expectedBool;
            case NOT_EQUALS:
                return metricBool != expectedBool;
            default:
                log.warn("Unknown boolean condition: {}", rule.getCondition());
                return false;
        }
    }
    
    /**
     * Evaluate string condition (EQUALS, NOT_EQUALS, CONTAINS)
     */
    private boolean evaluateStringCondition(String value, AlertRule rule) {
        if (rule.getThresholdText() == null) {
            log.warn("String rule {} has no threshold text", rule.getId());
            return false;
        }
        
        if (value == null) {
            value = "";
        }
        
        String expected = rule.getThresholdText();
        
        switch (rule.getCondition()) {
            case EQUALS:
            case EQ:
                return value.equals(expected);
            case NOT_EQUALS:
                return !value.equals(expected);
            case CONTAINS:
                return value.contains(expected);
            default:
                log.warn("Unknown string condition: {}", rule.getCondition());
                return false;
        }
    }
    
    private void handleTriggeredAlert(AlertRule rule, Metric metric) {
        // Get metric definition to determine type
        Optional<MetricDefinition> metricDefOpt = metricDefinitionRepository.findByMetricName(metric.getMetricType());
        String metricType = metricDefOpt.map(def -> def.getMetricType().name()).orElse("NUMERIC");
        
        // Only look for ACTIVE or ACKNOWLEDGED alerts (not RESOLVED)
        Alert existingAlert = alertRepository.findByAlertRuleIdAndAgentIdAndStatusIn(
                rule.getId(), 
                metric.getAgentId(), 
                java.util.Arrays.asList(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED)
        ).orElse(null);
        
        if (existingAlert != null) {
            // Update existing active or acknowledged alert based on metric type
            if ("NUMERIC".equals(metricType)) {
                existingAlert.setTriggerValue(metric.getValue());
            } else if ("STRING".equals(metricType)) {
                existingAlert.setTriggerValueText(metric.getTextValue());
            } else if ("BOOLEAN".equals(metricType)) {
                existingAlert.setTriggerValue(metric.getValue());
            }
            existingAlert.setLastTriggeredAt(LocalDateTime.now());
            existingAlert.setTriggerCount(existingAlert.getTriggerCount() + 1);
            alertRepository.save(existingAlert);
            
            log.debug("Updated existing {} alert for rule {} on agent {}", 
                    existingAlert.getStatus(), rule.getId(), metric.getAgentId());
        } else {
            // No active/acknowledged alert exists, create a new one
            // This includes the case where a previous alert was RESOLVED
            Alert newAlert;
            if ("NUMERIC".equals(metricType)) {
                newAlert = new Alert(
                        rule.getId(),
                        metric.getAgentId(),
                        rule.getName(),
                        rule.getMetricType(),
                        metric.getValue(),
                        rule.getThreshold() != null ? rule.getThreshold() : 0.0,
                        rule.getSeverity()
                );
            } else {
                // Boolean or String type
                String triggerText = "STRING".equals(metricType) ? 
                        metric.getTextValue() : 
                        (metric.getValue() > 0.5 ? "true" : "false");
                newAlert = new Alert(
                        rule.getId(),
                        metric.getAgentId(),
                        rule.getName(),
                        rule.getMetricType(),
                        triggerText,
                        rule.getThresholdText() != null ? rule.getThresholdText() : "",
                        rule.getSeverity()
                );
            }
            alertRepository.save(newAlert);
            
            log.info("Created new alert for rule {} on agent {}: metricType={}, condition={}, threshold={}/{}", 
                    rule.getId(), metric.getAgentId(), 
                    metricType, rule.getCondition(), rule.getThreshold(), rule.getThresholdText());
        }
    }
    
    /**
     * Auto-resolve alerts for this rule that were not triggered in the current cycle
     */
    private void autoResolveAlertsForRule(AlertRule rule, java.util.Set<String> triggeredAgents, List<Metric> recentMetrics) {
        // Find all active/acknowledged alerts for this rule
        List<Alert> alertsForRule = alertRepository.findByStatusIn(
                java.util.Arrays.asList(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED));
        
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
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolveNote("Auto-resolved: condition no longer met");
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
                
                log.info("Auto-resolved alert {} for agent {} - recent metrics no longer violate condition (rule: {})",
                        alert.getId(), alert.getAgentId(), rule.getName());
            } else {
                // No recent metrics for this agent - auto-resolve
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolveNote("Auto-resolved: no recent metrics");
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
                
                log.info("Auto-resolved alert {} for agent {} - no recent metrics (rule: {})",
                        alert.getId(), alert.getAgentId(), rule.getName());
            }
        }
    }
}

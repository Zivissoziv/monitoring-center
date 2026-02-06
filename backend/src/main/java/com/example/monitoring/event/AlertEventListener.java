package com.example.monitoring.event;

import com.example.monitoring.entity.Alert;
import com.example.monitoring.enums.AlertStatus;
import com.example.monitoring.repository.AlertRepository;
import com.example.monitoring.entity.AlertRule;
import com.example.monitoring.repository.AlertRuleRepository;
import com.example.monitoring.service.AlertService;
import com.example.monitoring.entity.Metric;
import com.example.monitoring.entity.MetricDefinition;
import com.example.monitoring.repository.MetricDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Listens for metric collection events and checks alert rules in real-time
 */
@Slf4j
@Component
public class AlertEventListener {
    
    @Autowired
    private AlertRuleRepository alertRuleRepository;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private MetricDefinitionRepository metricDefinitionRepository;
    
    @Autowired
    private AlertRepository alertRepository;
    
    /**
     * Process metric and check alert rules asynchronously
     */
    @Async
    @EventListener
    public void handleMetricCollected(MetricCollectedEvent event) {
        Metric metric = event.getMetric();
        
        log.debug("Processing metric event: agentId={}, metricType={}, value={}", 
                metric.getAgentId(), metric.getMetricType(), metric.getValue());
        
        try {
            // Get all enabled alert rules for this metric type
            List<AlertRule> rules = alertRuleRepository.findByMetricTypeAndEnabled(
                    metric.getMetricType(), true);
            
            log.debug("Found {} enabled rules for metric type: {}", 
                    rules.size(), metric.getMetricType());
            
            // Track which rules were triggered for this agent
            java.util.Set<Long> triggeredRuleIds = new java.util.HashSet<>();
            
            // Check each rule
            for (AlertRule rule : rules) {
                try {
                    // Skip if rule is for specific agent and doesn't match
                    if (rule.getAgentId() != null && !rule.getAgentId().equals(metric.getAgentId())) {
                        continue;
                    }
                    
                    // Evaluate the rule
                    boolean triggered = evaluateRule(metric, rule);
                    
                    if (triggered) {
                        log.info("Alert rule triggered: ruleId={}, ruleName={}, agentId={}, metricType={}", 
                                rule.getId(), rule.getName(), metric.getAgentId(), metric.getMetricType());
                        alertService.checkAlert(metric, rule);
                        triggeredRuleIds.add(rule.getId());
                    } else {
                        // Rule not triggered - check if we should auto-resolve
                        autoResolveAlertIfNeeded(rule, metric.getAgentId());
                    }
                } catch (Exception e) {
                    log.error("Error checking rule {} for metric: {}", rule.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error processing metric event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Auto-resolve alert if condition is no longer met
     */
    private void autoResolveAlertIfNeeded(AlertRule rule, String agentId) {
        try {
            // Find active or acknowledged alert for this rule and agent
            Optional<Alert> existingAlertOpt = alertRepository.findByAlertRuleIdAndAgentIdAndStatusIn(
                    rule.getId(),
                    agentId,
                    java.util.Arrays.asList(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED)
            );
            
            if (existingAlertOpt.isPresent()) {
                Alert alert = existingAlertOpt.get();
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolveNote("Auto-resolved: condition no longer met");
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
                
                log.info("Auto-resolved alert {} for agent {} - condition no longer met (rule: {})",
                        alert.getId(), agentId, rule.getName());
            }
        } catch (Exception e) {
            log.error("Error auto-resolving alert for rule {} and agent {}: {}",
                    rule.getId(), agentId, e.getMessage(), e);
        }
    }
    
    /**
     * Evaluate if metric triggers the alert rule
     */
    private boolean evaluateRule(Metric metric, AlertRule rule) {
        // Get metric definition to determine type
        Optional<MetricDefinition> metricDefOpt = metricDefinitionRepository.findByMetricName(metric.getMetricType());
        String metricType = metricDefOpt.map(def -> def.getMetricType().name()).orElse("NUMERIC");
        
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
    
    private boolean evaluateNumericCondition(double value, AlertRule rule) {
        if (rule.getThreshold() == null) {
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
                return false;
        }
    }
    
    private boolean evaluateBooleanCondition(double value, AlertRule rule) {
        if (rule.getThresholdText() == null) {
            return false;
        }
        
        boolean metricBool = value > 0.5;
        boolean expectedBool = "true".equalsIgnoreCase(rule.getThresholdText()) || 
                               "1".equals(rule.getThresholdText());
        
        switch (rule.getCondition()) {
            case EQUALS:
            case EQ:
                return metricBool == expectedBool;
            case NOT_EQUALS:
                return metricBool != expectedBool;
            default:
                return false;
        }
    }
    
    private boolean evaluateStringCondition(String value, AlertRule rule) {
        if (rule.getThresholdText() == null) {
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
                return false;
        }
    }
}

package com.example.monitoring.alert;

import com.example.monitoring.metric.Metric;
import com.example.monitoring.metric.MetricDefinition;
import com.example.monitoring.metric.MetricDefinitionRepository;
import com.example.monitoring.metric.MetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AlertService {

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private MetricDefinitionRepository metricDefinitionRepository;

    public List<AlertRule> getAllAlertRules() {
        return alertRuleRepository.findAll();
    }

    public AlertRule getAlertRuleById(Long id) {
        return alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert rule not found with id: " + id));
    }

    public AlertRule createAlertRule(AlertRule alertRule) {
        return alertRuleRepository.save(alertRule);
    }

    public AlertRule updateAlertRule(Long id, AlertRule alertRuleDetails) {
        AlertRule alertRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert rule not found with id: " + id));

        alertRule.setName(alertRuleDetails.getName());
        alertRule.setAgentId(alertRuleDetails.getAgentId());
        alertRule.setMetricType(alertRuleDetails.getMetricType());
        alertRule.setCondition(alertRuleDetails.getCondition());
        alertRule.setThreshold(alertRuleDetails.getThreshold());
        alertRule.setThresholdText(alertRuleDetails.getThresholdText());
        alertRule.setSeverity(alertRuleDetails.getSeverity());
        alertRule.setEnabled(alertRuleDetails.isEnabled());

        return alertRuleRepository.save(alertRule);
    }

    public void deleteAlertRule(Long id) {
        AlertRule alertRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert rule not found with id: " + id));

        alertRuleRepository.delete(alertRule);
    }

    public boolean checkAlert(Metric metric) {
        // Get metric definition to determine metric type
        Optional<MetricDefinition> metricDefOpt = metricDefinitionRepository.findByMetricName(metric.getMetricType());
        String metricType = metricDefOpt.map(MetricDefinition::getMetricType).orElse("NUMERIC");
        
        // Get all enabled alert rules for this metric type
        List<AlertRule> alertRules = alertRuleRepository.findByMetricTypeAndEnabledTrue(metric.getMetricType());

        boolean anyAlertTriggered = false;

        for (AlertRule rule : alertRules) {
            // Check if rule applies to this agent or to all agents
            if (rule.getAgentId() == null || rule.getAgentId().equals(metric.getAgentId())) {
                // Check condition based on metric type
                boolean alertTriggered = evaluateAlertCondition(metric, rule, metricType);

                if (alertTriggered) {
                    // Record or update alert (merge if exists)
                    recordAlert(rule, metric);
                    anyAlertTriggered = true;
                }
            }
        }

        return anyAlertTriggered;
    }
    
    /**
     * Evaluate alert condition based on metric type
     */
    private boolean evaluateAlertCondition(Metric metric, AlertRule rule, String metricType) {
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
     * Evaluate numeric condition
     */
    private boolean evaluateNumericCondition(double value, AlertRule rule) {
        if (rule.getThreshold() == null) {
            log.warn("Numeric rule {} has no threshold", rule.getId());
            return false;
        }
        
        double threshold = rule.getThreshold();
        switch (rule.getCondition()) {
            case "GT":
            case ">":
                return value > threshold;
            case "LT":
            case "<":
                return value < threshold;
            case "GTE":
            case ">=":
                return value >= threshold;
            case "LTE":
            case "<=":
                return value <= threshold;
            case "EQ":
            case "=":
            case "EQUALS":
                return Math.abs(value - threshold) < 0.001;
            case "NOT_EQUALS":
            case "!=":
                return Math.abs(value - threshold) >= 0.001;
            default:
                log.warn("Unknown numeric condition: {}", rule.getCondition());
                return false;
        }
    }
    
    /**
     * Evaluate boolean condition
     */
    private boolean evaluateBooleanCondition(double value, AlertRule rule) {
        if (rule.getThresholdText() == null) {
            log.warn("Boolean rule {} has no threshold text", rule.getId());
            return false;
        }
        
        boolean metricBool = value > 0.5; // 0 = false, 1 = true
        boolean expectedBool = "true".equalsIgnoreCase(rule.getThresholdText()) || "1".equals(rule.getThresholdText());
        
        switch (rule.getCondition()) {
            case "EQUALS":
            case "=":
            case "EQ":
                return metricBool == expectedBool;
            case "NOT_EQUALS":
            case "!=":
                return metricBool != expectedBool;
            default:
                log.warn("Unknown boolean condition: {}", rule.getCondition());
                return false;
        }
    }
    
    /**
     * Evaluate string condition
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
            case "EQUALS":
            case "=":
            case "EQ":
                return value.equals(expected);
            case "NOT_EQUALS":
            case "!=":
                return !value.equals(expected);
            case "CONTAINS":
                return value.contains(expected);
            case "NOT_CONTAINS":
                return !value.contains(expected);
            default:
                log.warn("Unknown string condition: {}", rule.getCondition());
                return false;
        }
    }

    /**
     * Record alert or update existing one (merge alerts for same rule and agent)
     * Note: If existing alert is RESOLVED, create a new alert instead of reactivating
     */
    private void recordAlert(AlertRule rule, Metric metric) {
        // Get metric definition to determine type
        Optional<MetricDefinition> metricDefOpt = metricDefinitionRepository.findByMetricName(metric.getMetricType());
        String metricType = metricDefOpt.map(MetricDefinition::getMetricType).orElse("NUMERIC");
        
        // Only look for ACTIVE or ACKNOWLEDGED alerts (not RESOLVED)
        Optional<Alert> existingAlert = alertRepository.findByAlertRuleIdAndAgentIdAndStatusIn(
                rule.getId(), 
                metric.getAgentId(),
                java.util.Arrays.asList("ACTIVE", "ACKNOWLEDGED"));

        if (existingAlert.isPresent()) {
            Alert alert = existingAlert.get();
            
            // Update existing active/acknowledged alert based on metric type
            if ("NUMERIC".equals(metricType)) {
                alert.setTriggerValue(metric.getValue());
            } else if ("STRING".equals(metricType)) {
                alert.setTriggerValueText(metric.getTextValue());
            } else if ("BOOLEAN".equals(metricType)) {
                alert.setTriggerValue(metric.getValue());
            }
            alert.setLastTriggeredAt(System.currentTimeMillis());
            alert.setTriggerCount(alert.getTriggerCount() + 1);
            alertRepository.save(alert);
        } else {
            // No active/acknowledged alert exists, create a new one
            // This includes the case where a previous alert was RESOLVED
            Alert alert;
            if ("NUMERIC".equals(metricType)) {
                alert = new Alert(
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
                alert = new Alert(
                        rule.getId(),
                        metric.getAgentId(),
                        rule.getName(),
                        rule.getMetricType(),
                        triggerText,
                        rule.getThresholdText() != null ? rule.getThresholdText() : "",
                        rule.getSeverity()
                );
            }
            alertRepository.save(alert);
        }
    }

    // Alert management methods

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public List<Alert> getActiveAlerts() {
        return alertRepository.findByStatusOrderByLastTriggeredAtDesc("ACTIVE");
    }

    public List<Alert> getAlertsByStatus(String status) {
        return alertRepository.findByStatusOrderByLastTriggeredAtDesc(status);
    }

    public Alert acknowledgeAlert(Long alertId, String acknowledgedBy) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(System.currentTimeMillis());

        return alertRepository.save(alert);
    }

    public Alert resolveAlert(Long alertId, String resolveNote) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        alert.setStatus("RESOLVED");
        alert.setResolveNote(resolveNote);
        alert.setResolvedAt(System.currentTimeMillis());

        return alertRepository.save(alert);
    }

    public void deleteAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        alertRepository.delete(alert);
    }
}
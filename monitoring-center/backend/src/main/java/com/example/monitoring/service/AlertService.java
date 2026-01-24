package com.example.monitoring.service;

import com.example.monitoring.entity.Agent;
import com.example.monitoring.entity.Alert;
import com.example.monitoring.entity.AlertRule;
import com.example.monitoring.entity.Metric;
import com.example.monitoring.entity.MetricDefinition;
import com.example.monitoring.enums.AlertCondition;
import com.example.monitoring.enums.AlertStatus;
import com.example.monitoring.enums.MetricValueType;
import com.example.monitoring.repository.AgentRepository;
import com.example.monitoring.repository.AlertRepository;
import com.example.monitoring.repository.AlertRuleRepository;
import com.example.monitoring.repository.MetricDefinitionRepository;
import com.example.monitoring.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final MetricRepository metricRepository;
    private final MetricDefinitionRepository metricDefinitionRepository;
    private final AgentRepository agentRepository;

    // ==================== AlertRule CRUD ====================
    
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
        alertRule.setAlertMessage(alertRuleDetails.getAlertMessage());
        alertRule.setEnabled(alertRuleDetails.isEnabled());

        return alertRuleRepository.save(alertRule);
    }

    public void deleteAlertRule(Long id) {
        AlertRule alertRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert rule not found with id: " + id));

        alertRuleRepository.delete(alertRule);
    }

    // ==================== Alert Check Logic ====================
    
    /**
     * Check alert for a specific metric and rule (called by event listener)
     */
    public boolean checkAlert(Metric metric, AlertRule rule) {
        // Get metric definition to determine metric type
        Optional<MetricDefinition> metricDefOpt = metricDefinitionRepository.findByMetricName(metric.getMetricType());
        MetricValueType metricType = metricDefOpt.map(MetricDefinition::getMetricType).orElse(MetricValueType.NUMERIC);
        
        // Record or update alert
        recordAlert(rule, metric, metricType);
        
        return true;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public boolean checkAlert(Metric metric) {
        // Get metric definition to determine metric type
        Optional<MetricDefinition> metricDefOpt = metricDefinitionRepository.findByMetricName(metric.getMetricType());
        MetricValueType metricType = metricDefOpt.map(MetricDefinition::getMetricType).orElse(MetricValueType.NUMERIC);
        
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
                    recordAlert(rule, metric, metricType);
                    anyAlertTriggered = true;
                }
            }
        }

        return anyAlertTriggered;
    }
    
    /**
     * Evaluate alert condition based on metric type
     */
    private boolean evaluateAlertCondition(Metric metric, AlertRule rule, MetricValueType metricType) {
        switch (metricType) {
            case NUMERIC:
                return evaluateNumericCondition(metric.getValue(), rule);
            case BOOLEAN:
                return evaluateBooleanCondition(metric.getValue(), rule);
            case STRING:
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
        AlertCondition condition = rule.getCondition();
        
        if (condition == null) {
            log.warn("Rule {} has no condition", rule.getId());
            return false;
        }
        
        switch (condition) {
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
                log.warn("Unknown numeric condition: {}", condition);
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
        
        AlertCondition condition = rule.getCondition();
        if (condition == null) {
            return false;
        }
        
        switch (condition) {
            case EQUALS:
            case EQ:
                return metricBool == expectedBool;
            case NOT_EQUALS:
                return metricBool != expectedBool;
            default:
                log.warn("Unknown boolean condition: {}", condition);
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
        AlertCondition condition = rule.getCondition();
        
        if (condition == null) {
            return false;
        }
        
        switch (condition) {
            case EQUALS:
            case EQ:
                return value.equals(expected);
            case NOT_EQUALS:
                return !value.equals(expected);
            case CONTAINS:
                return value.contains(expected);
            default:
                log.warn("Unknown string condition: {}", condition);
                return false;
        }
    }

    /**
     * Record alert or update existing one (merge alerts for same rule and agent)
     * Note: If existing alert is RESOLVED, create a new alert instead of reactivating
     */
    private void recordAlert(AlertRule rule, Metric metric, MetricValueType metricType) {
        // Only look for ACTIVE or ACKNOWLEDGED alerts (not RESOLVED)
        Optional<Alert> existingAlert = alertRepository.findByAlertRuleIdAndAgentIdAndStatusIn(
                rule.getId(), 
                metric.getAgentId(),
                Arrays.asList(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED));

        if (existingAlert.isPresent()) {
            Alert alert = existingAlert.get();
            
            // Update existing active/acknowledged alert based on metric type
            if (metricType == MetricValueType.NUMERIC || metricType == MetricValueType.BOOLEAN) {
                alert.setTriggerValue(metric.getValue());
            } else if (metricType == MetricValueType.STRING) {
                alert.setTriggerValueText(metric.getTextValue());
            }
            alert.setLastTriggeredAt(LocalDateTime.now());
            alert.setTriggerCount(alert.getTriggerCount() + 1);
            alertRepository.save(alert);
        } else {
            // No active/acknowledged alert exists, create a new one
            Alert alert;
            if (metricType == MetricValueType.NUMERIC) {
                alert = new Alert(
                        rule.getId(),
                        metric.getAgentId(),
                        rule.getName(),
                        rule.getMetricType(),
                        metric.getValue(),
                        rule.getThreshold() != null ? rule.getThreshold() : 0.0,
                        rule.getSeverity()
                );
                alert.setAlertMessage(rule.getAlertMessage());
            } else {
                // Boolean or String type
                String triggerText = metricType == MetricValueType.STRING ? 
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
                alert.setAlertMessage(rule.getAlertMessage());
            }
            
            // Set appCode from metric or agent
            if (metric.getAppCode() != null) {
                alert.setAppCode(metric.getAppCode());
            } else {
                agentRepository.findById(metric.getAgentId())
                        .ifPresent(agent -> alert.setAppCode(agent.getAppCode()));
            }
            
            alertRepository.save(alert);
        }
    }

    // ==================== Alert CRUD ====================

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public List<Alert> getActiveAlerts() {
        return alertRepository.findByStatusOrderByLastTriggeredAtDesc(AlertStatus.ACTIVE);
    }

    public List<Alert> getAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatusOrderByLastTriggeredAtDesc(status);
    }

    public Alert acknowledgeAlert(Long alertId, String acknowledgedBy) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(LocalDateTime.now());

        return alertRepository.save(alert);
    }

    public Alert resolveAlert(Long alertId, String resolveNote) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolveNote(resolveNote);
        alert.setResolvedAt(LocalDateTime.now());

        return alertRepository.save(alert);
    }

    public void deleteAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        alertRepository.delete(alert);
    }
    
    // ==================== Filter by App Codes ====================
    
    public List<Alert> getAlertsByAppCodes(List<String> appCodes) {
        if (appCodes == null || appCodes.isEmpty()) {
            return alertRepository.findAll();
        }
        return alertRepository.findByAppCodeIn(appCodes);
    }
    
    public List<Alert> getActiveAlertsByAppCodes(List<String> appCodes) {
        if (appCodes == null || appCodes.isEmpty()) {
            return alertRepository.findByStatusOrderByLastTriggeredAtDesc(AlertStatus.ACTIVE);
        }
        return alertRepository.findByAppCodeInAndStatusOrderByLastTriggeredAtDesc(appCodes, AlertStatus.ACTIVE);
    }
    
    public List<Alert> getAlertsByAppCodesAndStatuses(List<String> appCodes, List<AlertStatus> statuses) {
        if (appCodes == null || appCodes.isEmpty()) {
            return alertRepository.findByStatusIn(statuses);
        }
        return alertRepository.findByAppCodeInAndStatusInOrderByLastTriggeredAtDesc(appCodes, statuses);
    }
}
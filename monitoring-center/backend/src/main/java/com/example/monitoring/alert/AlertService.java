package com.example.monitoring.alert;

import com.example.monitoring.metric.Metric;
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
        // Get all enabled alert rules for this metric type
        List<AlertRule> alertRules = alertRuleRepository.findByMetricTypeAndEnabledTrue(metric.getMetricType());

        boolean anyAlertTriggered = false;

        for (AlertRule rule : alertRules) {
            // Check if rule applies to this agent or to all agents
            if (rule.getAgentId() == null || rule.getAgentId().equals(metric.getAgentId())) {
                // Check condition
                boolean alertTriggered = false;
                switch (rule.getCondition()) {
                    case "GT":
                        alertTriggered = metric.getValue() > rule.getThreshold();
                        break;
                    case "LT":
                        alertTriggered = metric.getValue() < rule.getThreshold();
                        break;
                    case "EQ":
                        alertTriggered = metric.getValue() == rule.getThreshold();
                        break;
                }

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
     * Record alert or update existing one (merge alerts for same rule and agent)
     * Note: If existing alert is RESOLVED, create a new alert instead of reactivating
     */
    private void recordAlert(AlertRule rule, Metric metric) {
        Optional<Alert> existingAlert = alertRepository.findByAlertRuleIdAndAgentId(
                rule.getId(), metric.getAgentId());

        if (existingAlert.isPresent()) {
            Alert alert = existingAlert.get();
            
            // If alert was RESOLVED (closed), create a new alert instead of reactivating
            if ("RESOLVED".equals(alert.getStatus())) {
                // Delete the UNIQUE constraint entry by removing the old resolved alert
                alertRepository.delete(alert);
                
                // Create new alert
                Alert newAlert = new Alert(
                        rule.getId(),
                        metric.getAgentId(),
                        rule.getName(),
                        metric.getMetricType(),
                        metric.getValue(),
                        rule.getThreshold(),
                        rule.getSeverity()
                );
                alertRepository.save(newAlert);
            } else {
                // Update existing active/acknowledged alert
                alert.setTriggerValue(metric.getValue());
                alert.setLastTriggeredAt(System.currentTimeMillis());
                alert.setTriggerCount(alert.getTriggerCount() + 1);
                alertRepository.save(alert);
            }
        } else {
            // Create new alert
            Alert alert = new Alert(
                    rule.getId(),
                    metric.getAgentId(),
                    rule.getName(),
                    metric.getMetricType(),
                    metric.getValue(),
                    rule.getThreshold(),
                    rule.getSeverity()
            );
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

        return alertRepository.save(alert);
    }

    public void deleteAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        alertRepository.delete(alert);
    }
}
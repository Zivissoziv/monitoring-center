package com.example.monitoring.alert;

import com.example.monitoring.metric.Metric;
import com.example.monitoring.metric.MetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {
    
    @Autowired
    private AlertRuleRepository alertRuleRepository;
    
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
                    // In a real implementation, we would send an alert notification here
                    System.out.println("ALERT TRIGGERED: " + rule.getName() + 
                            " for agent " + metric.getAgentId() + 
                            " - Metric " + metric.getMetricType() + 
                            " value " + metric.getValue() + 
                            " " + rule.getCondition() + 
                            " threshold " + rule.getThreshold());
                    return true;
                }
            }
        }
        
        return false;
    }
}
package com.example.monitoring.controller;

import com.example.monitoring.entity.AlertRule;
import com.example.monitoring.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {
    
    @Autowired
private AlertService alertService;
    
    @GetMapping("/rules")
    public List<AlertRule> getAllAlertRules() {
        return alertService.getAllAlertRules();
    }
    
    @GetMapping("/rules/{id}")
    public ResponseEntity<AlertRule> getAlertRuleById(@PathVariable Long id) {
        try {
            AlertRule alertRule = alertService.getAlertRuleById(id);
            return ResponseEntity.ok(alertRule);
        } catch (RuntimeException e) {
            log.error("Alert rule {} not found", id, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/rules")
    public AlertRule createAlertRule(@RequestBody AlertRule alertRule) {
        log.info("POST /api/alerts/rules - Creating alert rule: {}", alertRule.getName());
        AlertRule result = alertService.createAlertRule(alertRule);
        log.info("POST /api/alerts/rules - Success");
        return result;
    }
    
    @PutMapping("/rules/{id}")
    public ResponseEntity<AlertRule> updateAlertRule(@PathVariable Long id, @RequestBody AlertRule alertRuleDetails) {
        try {
            AlertRule updatedAlertRule = alertService.updateAlertRule(id, alertRuleDetails);
            return ResponseEntity.ok(updatedAlertRule);
        } catch (RuntimeException e) {
            log.error("Failed to update alert rule {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteAlertRule(@PathVariable Long id) {
        try {
            alertService.deleteAlertRule(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Failed to delete alert rule {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}
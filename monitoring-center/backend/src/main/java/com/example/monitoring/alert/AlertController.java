package com.example.monitoring.alert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/rules")
    public AlertRule createAlertRule(@RequestBody AlertRule alertRule) {
        return alertService.createAlertRule(alertRule);
    }
    
    @PutMapping("/rules/{id}")
    public ResponseEntity<AlertRule> updateAlertRule(@PathVariable Long id, @RequestBody AlertRule alertRuleDetails) {
        try {
            AlertRule updatedAlertRule = alertService.updateAlertRule(id, alertRuleDetails);
            return ResponseEntity.ok(updatedAlertRule);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteAlertRule(@PathVariable Long id) {
        try {
            alertService.deleteAlertRule(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
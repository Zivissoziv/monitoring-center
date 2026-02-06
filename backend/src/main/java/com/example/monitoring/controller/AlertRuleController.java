package com.example.monitoring.controller;

import com.example.monitoring.entity.AlertRule;
import com.example.monitoring.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警规则管理接口 (独立资源)
 */
@Slf4j
@RestController
@RequestMapping("/api/alert-rules")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AlertRuleController {
    
    private final AlertService alertService;
    
    @GetMapping
    public ResponseEntity<List<AlertRule>> getAllAlertRules() {
        return ResponseEntity.ok(alertService.getAllAlertRules());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AlertRule> getAlertRuleById(@PathVariable Long id) {
        try {
            AlertRule alertRule = alertService.getAlertRuleById(id);
            return ResponseEntity.ok(alertRule);
        } catch (RuntimeException e) {
            log.error("Alert rule {} not found", id, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<AlertRule> createAlertRule(@RequestBody AlertRule alertRule) {
        log.info("POST /api/alert-rules - Creating alert rule: {}", alertRule.getName());
        AlertRule result = alertService.createAlertRule(alertRule);
        log.info("POST /api/alert-rules - Success, id: {}", result.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AlertRule> updateAlertRule(@PathVariable Long id, @RequestBody AlertRule alertRuleDetails) {
        try {
            AlertRule updatedAlertRule = alertService.updateAlertRule(id, alertRuleDetails);
            return ResponseEntity.ok(updatedAlertRule);
        } catch (RuntimeException e) {
            log.error("Failed to update alert rule {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
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

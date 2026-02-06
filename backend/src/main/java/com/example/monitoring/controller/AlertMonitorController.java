package com.example.monitoring.controller;

import com.example.monitoring.entity.Alert;
import com.example.monitoring.enums.AlertStatus;
import com.example.monitoring.security.FilterByUserApp;
import com.example.monitoring.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警监控接口
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AlertMonitorController {
    
    private final AlertService alertService;
    
    @GetMapping
    @FilterByUserApp
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }
    
    @GetMapping("/active")
    @FilterByUserApp
    public ResponseEntity<List<Alert>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }
    
    @GetMapping("/status/{status}")
    @FilterByUserApp
    public ResponseEntity<List<Alert>> getAlertsByStatus(@PathVariable String status) {
        try {
            AlertStatus alertStatus = AlertStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(alertService.getAlertsByStatus(alertStatus));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid alert status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlert(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        log.info("POST /api/alerts/{}/acknowledge", id);
        try {
            String acknowledgedBy = request.getOrDefault("acknowledgedBy", "System");
            Alert alert = alertService.acknowledgeAlert(id, acknowledgedBy);
            log.info("POST /api/alerts/{}/acknowledge - Success", id);
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            log.error("POST /api/alerts/{}/acknowledge - Failed: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Alert> resolveAlert(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        log.info("POST /api/alerts/{}/resolve", id);
        try {
            String resolveNote = request.getOrDefault("resolveNote", "");
            Alert alert = alertService.resolveAlert(id, resolveNote);
            log.info("POST /api/alerts/{}/resolve - Success", id);
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            log.error("POST /api/alerts/{}/resolve - Failed: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        log.info("DELETE /api/alerts/{}", id);
        try {
            alertService.deleteAlert(id);
            log.info("DELETE /api/alerts/{} - Success", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("DELETE /api/alerts/{} - Failed: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}

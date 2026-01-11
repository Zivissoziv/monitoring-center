package com.example.monitoring.controller;

import com.example.monitoring.entity.Alert;
import com.example.monitoring.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertMonitorController {
    
    @Autowired
    private AlertService alertService;
    
    @GetMapping
    public List<Alert> getAllAlerts() {
        return alertService.getAllAlerts();
    }
    
    @GetMapping("/active")
    public List<Alert> getActiveAlerts() {
        return alertService.getActiveAlerts();
    }
    
    @GetMapping("/status/{status}")
    public List<Alert> getAlertsByStatus(@PathVariable String status) {
        return alertService.getAlertsByStatus(status);
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
package com.example.monitoring.alert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        System.out.println("[API] POST /api/alerts/" + id + "/acknowledge - Acknowledging alert");
        try {
            String acknowledgedBy = request.getOrDefault("acknowledgedBy", "System");
            Alert alert = alertService.acknowledgeAlert(id, acknowledgedBy);
            System.out.println("[API] POST /api/alerts/" + id + "/acknowledge - Success");
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            System.out.println("[API] POST /api/alerts/" + id + "/acknowledge - Failed: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Alert> resolveAlert(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        System.out.println("[API] POST /api/alerts/" + id + "/resolve - Resolving alert");
        try {
            String resolveNote = request.getOrDefault("resolveNote", "");
            Alert alert = alertService.resolveAlert(id, resolveNote);
            System.out.println("[API] POST /api/alerts/" + id + "/resolve - Success");
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            System.out.println("[API] POST /api/alerts/" + id + "/resolve - Failed: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        System.out.println("[API] DELETE /api/alerts/" + id + " - Deleting alert");
        try {
            alertService.deleteAlert(id);
            System.out.println("[API] DELETE /api/alerts/" + id + " - Success");
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            System.out.println("[API] DELETE /api/alerts/" + id + " - Failed: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
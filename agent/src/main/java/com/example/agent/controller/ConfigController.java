package com.example.agent.controller;

import com.example.agent.dto.MetricCollectionConfig;
import com.example.agent.scheduler.MetricScheduler;
import com.example.agent.service.MetricCollectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for receiving metric collection configurations from backend
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
public class ConfigController {
    
    @Autowired
    private MetricCollectionService metricCollectionService;
    
    @Autowired
    private MetricScheduler metricScheduler;
    
    /**
     * Receive and update metric collection configurations from backend
     */
    @PostMapping("/metrics")
    public ResponseEntity<String> updateMetricConfigs(@RequestBody List<MetricCollectionConfig> configs) {
        log.info("Received metric config update from backend, count: {}", configs.size());
        
        try {
            metricCollectionService.updateConfigs(configs);
            metricScheduler.resetAll();  // Reset collection times after config update
            
            log.info("Successfully updated metric configurations");
            return ResponseEntity.ok("Configuration updated successfully");
        } catch (Exception e) {
            log.error("Failed to update metric configurations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get current metric configurations
     */
    @GetMapping("/metrics")
    public ResponseEntity<List<MetricCollectionConfig>> getMetricConfigs() {
        log.info("Get metric configurations request");
        List<MetricCollectionConfig> configs = metricCollectionService.getConfigs();
        return ResponseEntity.ok(configs);
    }
}

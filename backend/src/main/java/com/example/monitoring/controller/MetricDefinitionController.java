package com.example.monitoring.controller;

import com.example.monitoring.entity.MetricDefinition;
import com.example.monitoring.service.MetricDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/metric-definitions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MetricDefinitionController {
    
    private final MetricDefinitionService metricDefinitionService;
    
    @GetMapping
    public ResponseEntity<List<MetricDefinition>> getAllDefinitions() {
        return ResponseEntity.ok(metricDefinitionService.getAllDefinitions());
    }
    
    @GetMapping("/enabled")
    public ResponseEntity<List<MetricDefinition>> getEnabledDefinitions() {
        return ResponseEntity.ok(metricDefinitionService.getEnabledDefinitions());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MetricDefinition> getDefinitionById(@PathVariable Long id) {
        return metricDefinitionService.getDefinitionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/name/{metricName}")
    public ResponseEntity<MetricDefinition> getDefinitionByName(@PathVariable String metricName) {
        return metricDefinitionService.getDefinitionByName(metricName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<MetricDefinition> createDefinition(@RequestBody MetricDefinition definition) {
        MetricDefinition created = metricDefinitionService.createDefinition(definition);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MetricDefinition> updateDefinition(@PathVariable Long id, @RequestBody MetricDefinition definitionDetails) {
        try {
            MetricDefinition updated = metricDefinitionService.updateDefinition(id, definitionDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Failed to update metric definition {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
        try {
            metricDefinitionService.deleteDefinition(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Failed to delete metric definition {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}

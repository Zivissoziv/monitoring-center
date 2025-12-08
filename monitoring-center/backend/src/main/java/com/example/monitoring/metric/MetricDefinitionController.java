package com.example.monitoring.metric;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metric-definitions")
@CrossOrigin(origins = "*")
public class MetricDefinitionController {
    
    @Autowired
    private MetricDefinitionService metricDefinitionService;
    
    @GetMapping
    public List<MetricDefinition> getAllDefinitions() {
        return metricDefinitionService.getAllDefinitions();
    }
    
    @GetMapping("/enabled")
    public List<MetricDefinition> getEnabledDefinitions() {
        return metricDefinitionService.getEnabledDefinitions();
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
    public MetricDefinition createDefinition(@RequestBody MetricDefinition definition) {
        return metricDefinitionService.createDefinition(definition);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MetricDefinition> updateDefinition(@PathVariable Long id, @RequestBody MetricDefinition definitionDetails) {
        try {
            MetricDefinition updated = metricDefinitionService.updateDefinition(id, definitionDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
        try {
            metricDefinitionService.deleteDefinition(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

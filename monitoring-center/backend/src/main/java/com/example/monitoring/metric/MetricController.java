package com.example.monitoring.metric;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricController {
    
    @Autowired
    private MetricService metricService;
    
    @GetMapping
    public List<Metric> getAllMetrics() {
        return metricService.getAllMetrics();
    }
    
    @GetMapping("/agent/{agentId}")
    public List<Metric> getMetricsByAgentId(@PathVariable Long agentId) {
        return metricService.getMetricsByAgentId(agentId);
    }
    
    @GetMapping("/agent/{agentId}/type/{metricType}")
    public List<Metric> getMetricsByAgentIdAndType(@PathVariable Long agentId, @PathVariable String metricType) {
        return metricService.getMetricsByAgentIdAndType(agentId, metricType);
    }
    
    @PostMapping
    public Metric saveMetric(@RequestBody Metric metric) {
        System.out.println("[API] POST /api/metrics - Saving metric for agent: " + metric.getAgentId());
        Metric result = metricService.saveMetric(metric);
        System.out.println("[API] POST /api/metrics - Success");
        return result;
    }
    
    @PostMapping("/batch")
    public List<Metric> saveMetrics(@RequestBody List<Metric> metrics) {
        System.out.println("[API] POST /api/metrics/batch - Saving " + metrics.size() + " metrics");
        List<Metric> result = metricService.saveMetrics(metrics);
        System.out.println("[API] POST /api/metrics/batch - Success");
        return result;
    }
}
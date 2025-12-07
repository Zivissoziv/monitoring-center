package com.example.monitoring.metric;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    
    // Add paginated endpoint
    @GetMapping("/paginated")
    public Page<Metric> getAllMetricsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return metricService.getAllMetrics(pageable);
    }
    
    @GetMapping("/agent/{agentId}")
    public List<Metric> getMetricsByAgentId(@PathVariable Long agentId) {
        return metricService.getMetricsByAgentId(agentId);
    }
    
    // Add paginated endpoint
    @GetMapping("/agent/{agentId}/paginated")
    public Page<Metric> getMetricsByAgentIdPaginated(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return metricService.getMetricsByAgentId(agentId, pageable);
    }
    
    @GetMapping("/agent/{agentId}/type/{metricType}")
    public List<Metric> getMetricsByAgentIdAndType(@PathVariable Long agentId, @PathVariable String metricType) {
        return metricService.getMetricsByAgentIdAndType(agentId, metricType);
    }
    
    // Add paginated endpoint
    @GetMapping("/agent/{agentId}/type/{metricType}/paginated")
    public Page<Metric> getMetricsByAgentIdAndTypePaginated(
            @PathVariable Long agentId,
            @PathVariable String metricType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return metricService.getMetricsByAgentIdAndType(agentId, metricType, pageable);
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
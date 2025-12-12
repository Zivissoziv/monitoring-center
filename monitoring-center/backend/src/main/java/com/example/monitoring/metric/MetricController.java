package com.example.monitoring.metric;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
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
    public List<Metric> getMetricsByAgentId(@PathVariable String agentId) {
        return metricService.getMetricsByAgentId(agentId);
    }
    
    // Add paginated endpoint
    @GetMapping("/agent/{agentId}/paginated")
    public Page<Metric> getMetricsByAgentIdPaginated(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return metricService.getMetricsByAgentId(agentId, pageable);
    }
    
    @GetMapping("/agent/{agentId}/type/{metricType}")
    public List<Metric> getMetricsByAgentIdAndType(@PathVariable String agentId, @PathVariable String metricType) {
        return metricService.getMetricsByAgentIdAndType(agentId, metricType);
    }
    
    // Add paginated endpoint
    @GetMapping("/agent/{agentId}/type/{metricType}/paginated")
    public Page<Metric> getMetricsByAgentIdAndTypePaginated(
            @PathVariable String agentId,
            @PathVariable String metricType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return metricService.getMetricsByAgentIdAndType(agentId, metricType, pageable);
    }
    
    // Time range query endpoints
    @GetMapping("/agent/{agentId}/timerange")
    public List<Metric> getMetricsByAgentIdAndTimeRange(
            @PathVariable String agentId,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {
        return metricService.getMetricsByAgentIdAndTimeRange(agentId, startTime, endTime);
    }
    
    @GetMapping("/agent/{agentId}/timerange/paginated")
    public Page<Metric> getMetricsByAgentIdAndTimeRangePaginated(
            @PathVariable String agentId,
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return metricService.getMetricsByAgentIdAndTimeRange(agentId, startTime, endTime, pageable);
    }
    
    @GetMapping("/agent/{agentId}/type/{metricType}/timerange")
    public List<Metric> getMetricsByAgentIdAndTypeAndTimeRange(
            @PathVariable String agentId,
            @PathVariable String metricType,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {
        return metricService.getMetricsByAgentIdAndTypeAndTimeRange(agentId, metricType, startTime, endTime);
    }
    
    @GetMapping("/agent/{agentId}/type/{metricType}/timerange/paginated")
    public Page<Metric> getMetricsByAgentIdAndTypeAndTimeRangePaginated(
            @PathVariable String agentId,
            @PathVariable String metricType,
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return metricService.getMetricsByAgentIdAndTypeAndTimeRange(agentId, metricType, startTime, endTime, pageable);
    }
    
    @PostMapping
    public Metric saveMetric(@RequestBody Metric metric) {
        log.info("POST /api/metrics - Saving metric for agent: {}", metric.getAgentId());
        Metric result = metricService.saveMetric(metric);
        log.info("POST /api/metrics - Success");
        return result;
    }
    
    @PostMapping("/batch")
    public List<Metric> saveMetrics(@RequestBody List<Metric> metrics) {
        log.info("POST /api/metrics/batch - Saving {} metrics", metrics.size());
        List<Metric> result = metricService.saveMetrics(metrics);
        log.info("POST /api/metrics/batch - Success");
        return result;
    }
}
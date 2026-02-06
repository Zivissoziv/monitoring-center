package com.example.monitoring.controller;

import com.example.monitoring.entity.Metric;
import com.example.monitoring.security.FilterByUserApp;
import com.example.monitoring.service.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MetricController {
    
    private final MetricService metricService;
    
    /**
     * 获取所有指标（支持分页）
     */
    @GetMapping
    @FilterByUserApp
    public ResponseEntity<?> getAllMetrics(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(metricService.getAllMetrics(pageable));
        }
        return ResponseEntity.ok(metricService.getAllMetrics());
    }
    
    /**
     * 根据 Agent ID 获取指标（支持分页）
     */
    @GetMapping("/agent/{agentId}")
    @FilterByUserApp
    public ResponseEntity<?> getMetricsByAgentId(
            @PathVariable String agentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(metricService.getMetricsByAgentId(agentId, pageable));
        }
        return ResponseEntity.ok(metricService.getMetricsByAgentId(agentId));
    }
    
    /**
     * 根据 Agent ID 和指标类型获取指标（支持分页）
     */
    @GetMapping("/agent/{agentId}/type/{metricType}")
    @FilterByUserApp
    public ResponseEntity<?> getMetricsByAgentIdAndType(
            @PathVariable String agentId, 
            @PathVariable String metricType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(metricService.getMetricsByAgentIdAndType(agentId, metricType, pageable));
        }
        return ResponseEntity.ok(metricService.getMetricsByAgentIdAndType(agentId, metricType));
    }
    
    /**
     * 根据 Agent ID 和时间范围获取指标（支持分页）
     */
    @GetMapping("/agent/{agentId}/timerange")
    @FilterByUserApp
    public ResponseEntity<?> getMetricsByAgentIdAndTimeRange(
            @PathVariable String agentId,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(metricService.getMetricsByAgentIdAndTimeRange(agentId, startTime, endTime, pageable));
        }
        return ResponseEntity.ok(metricService.getMetricsByAgentIdAndTimeRange(agentId, startTime, endTime));
    }
    
    /**
     * 根据 Agent ID、指标类型和时间范围获取指标（支持分页）
     */
    @GetMapping("/agent/{agentId}/type/{metricType}/timerange")
    @FilterByUserApp
    public ResponseEntity<?> getMetricsByAgentIdAndTypeAndTimeRange(
            @PathVariable String agentId,
            @PathVariable String metricType,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(metricService.getMetricsByAgentIdAndTypeAndTimeRange(agentId, metricType, startTime, endTime, pageable));
        }
        return ResponseEntity.ok(metricService.getMetricsByAgentIdAndTypeAndTimeRange(agentId, metricType, startTime, endTime));
    }
    
    @PostMapping
    public ResponseEntity<Metric> saveMetric(@RequestBody Metric metric) {
        log.info("POST /api/metrics - Saving metric for agent: {}", metric.getAgentId());
        Metric result = metricService.saveMetric(metric);
        log.info("POST /api/metrics - Success");
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @PostMapping("/batch")
    public ResponseEntity<List<Metric>> saveMetrics(@RequestBody List<Metric> metrics) {
        log.info("POST /api/metrics/batch - Saving {} metrics", metrics.size());
        List<Metric> result = metricService.saveMetrics(metrics);
        log.info("POST /api/metrics/batch - Success");
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}

package com.example.monitoring.service;

import com.example.monitoring.entity.Agent;
import com.example.monitoring.entity.Metric;
import com.example.monitoring.repository.AgentRepository;
import com.example.monitoring.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricService {
    
    private final MetricRepository metricRepository;
    private final AgentRepository agentRepository;
    private final AlertService alertService;
    
    public List<Metric> getAllMetrics() {
        return metricRepository.findAllByOrderByTimestampDesc(Pageable.unpaged()).getContent();
    }
    
    public Page<Metric> getAllMetrics(Pageable pageable) {
        return metricRepository.findAllByOrderByTimestampDesc(pageable);
    }
    
    public List<Metric> getMetricsByAgentId(String agentId) {
        return metricRepository.findByAgentIdOrderByTimestampDesc(agentId);
    }
    
    public Page<Metric> getMetricsByAgentId(String agentId, Pageable pageable) {
        return metricRepository.findByAgentIdOrderByTimestampDesc(agentId, pageable);
    }
    
    public List<Metric> getMetricsByAgentIdAndType(String agentId, String metricType) {
        return metricRepository.findByAgentIdAndMetricTypeOrderByTimestampDesc(agentId, metricType);
    }
    
    public Page<Metric> getMetricsByAgentIdAndType(String agentId, String metricType, Pageable pageable) {
        return metricRepository.findByAgentIdAndMetricTypeOrderByTimestampDesc(agentId, metricType, pageable);
    }
    
    public List<Metric> getMetricsByAgentIdAndTimeRange(String agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return metricRepository.findByAgentIdAndTimestampBetweenOrderByTimestampDesc(agentId, startTime, endTime);
    }
    
    public List<Metric> getMetricsByAgentIdAndTypeAndTimeRange(String agentId, String metricType, LocalDateTime startTime, LocalDateTime endTime) {
        return metricRepository.findByAgentIdAndMetricTypeAndTimestampBetweenOrderByTimestampDesc(agentId, metricType, startTime, endTime);
    }
    
    public Page<Metric> getMetricsByAgentIdAndTimeRange(String agentId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return metricRepository.findByAgentIdAndTimestampBetweenOrderByTimestampDesc(agentId, startTime, endTime, pageable);
    }
    
    public Page<Metric> getMetricsByAgentIdAndTypeAndTimeRange(String agentId, String metricType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return metricRepository.findByAgentIdAndMetricTypeAndTimestampBetweenOrderByTimestampDesc(agentId, metricType, startTime, endTime, pageable);
    }
    
    public Metric saveMetric(Metric metric) {
        // Check if agent exists and set appCode from agent
        Agent agent = agentRepository.findById(metric.getAgentId())
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + metric.getAgentId()));
        
        // Set appCode from agent
        metric.setAppCode(agent.getAppCode());
        
        Metric savedMetric = metricRepository.save(metric);
        
        // Check if any alerts should be triggered
        alertService.checkAlert(savedMetric);
        
        return savedMetric;
    }
    
    public List<Metric> saveMetrics(List<Metric> metrics) {
        // Set appCode for each metric from agent
        for (Metric metric : metrics) {
            agentRepository.findById(metric.getAgentId())
                    .ifPresent(agent -> metric.setAppCode(agent.getAppCode()));
        }
        
        List<Metric> savedMetrics = metricRepository.saveAll(metrics);
        
        // Check if any alerts should be triggered for each metric
        for (Metric metric : savedMetrics) {
            alertService.checkAlert(metric);
        }
        
        return savedMetrics;
    }
    
    // Filter metrics by accessible app codes
    public List<Metric> getMetricsByAppCodes(List<String> appCodes) {
        if (appCodes == null || appCodes.isEmpty()) {
            return getAllMetrics();
        }
        return metricRepository.findByAppCodeInOrderByTimestampDesc(appCodes, Pageable.unpaged()).getContent();
    }
    
    public Page<Metric> getMetricsByAppCodes(List<String> appCodes, Pageable pageable) {
        if (appCodes == null || appCodes.isEmpty()) {
            return metricRepository.findAllByOrderByTimestampDesc(pageable);
        }
        return metricRepository.findByAppCodeInOrderByTimestampDesc(appCodes, pageable);
    }
    
    public Page<Metric> getMetricsByAppCodesAndAgentId(List<String> appCodes, String agentId, Pageable pageable) {
        if (appCodes == null || appCodes.isEmpty()) {
            return metricRepository.findByAgentIdOrderByTimestampDesc(agentId, pageable);
        }
        return metricRepository.findByAppCodeInAndAgentIdOrderByTimestampDesc(appCodes, agentId, pageable);
    }
}
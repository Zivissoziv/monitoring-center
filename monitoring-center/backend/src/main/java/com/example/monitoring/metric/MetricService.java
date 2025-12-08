package com.example.monitoring.metric;

import com.example.monitoring.agent.Agent;
import com.example.monitoring.agent.AgentRepository;
import com.example.monitoring.alert.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetricService {
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private AlertService alertService;
    
    public List<Metric> getAllMetrics() {
        return metricRepository.findAll();
    }
    
    // Add paginated method
    public Page<Metric> getAllMetrics(Pageable pageable) {
        return metricRepository.findAllByOrderByTimestampDesc(pageable);
    }
    
    public List<Metric> getMetricsByAgentId(String agentId) {
        return metricRepository.findByAgentIdOrderByTimestampDesc(agentId);
    }
    
    // Add paginated method
    public Page<Metric> getMetricsByAgentId(String agentId, Pageable pageable) {
        return metricRepository.findByAgentIdOrderByTimestampDesc(agentId, pageable);
    }
    
    public List<Metric> getMetricsByAgentIdAndType(String agentId, String metricType) {
        return metricRepository.findByAgentIdAndMetricTypeOrderByTimestampDesc(agentId, metricType);
    }
    
    // Add paginated method
    public Page<Metric> getMetricsByAgentIdAndType(String agentId, String metricType, Pageable pageable) {
        return metricRepository.findByAgentIdAndMetricTypeOrderByTimestampDesc(agentId, metricType, pageable);
    }
    
    // Add time range query methods
    public List<Metric> getMetricsByAgentIdAndTimeRange(String agentId, Long startTime, Long endTime) {
        return metricRepository.findByAgentIdAndTimestampBetweenOrderByTimestampDesc(agentId, startTime, endTime);
    }
    
    public List<Metric> getMetricsByAgentIdAndTypeAndTimeRange(String agentId, String metricType, Long startTime, Long endTime) {
        return metricRepository.findByAgentIdAndMetricTypeAndTimestampBetweenOrderByTimestampDesc(agentId, metricType, startTime, endTime);
    }
    
    public Page<Metric> getMetricsByAgentIdAndTimeRange(String agentId, Long startTime, Long endTime, Pageable pageable) {
        return metricRepository.findByAgentIdAndTimestampBetweenOrderByTimestampDesc(agentId, startTime, endTime, pageable);
    }
    
    public Page<Metric> getMetricsByAgentIdAndTypeAndTimeRange(String agentId, String metricType, Long startTime, Long endTime, Pageable pageable) {
        return metricRepository.findByAgentIdAndMetricTypeAndTimestampBetweenOrderByTimestampDesc(agentId, metricType, startTime, endTime, pageable);
    }
    
    public Metric saveMetric(Metric metric) {
        // Check if agent exists
        Agent agent = agentRepository.findById(metric.getAgentId())
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + metric.getAgentId()));
        
        Metric savedMetric = metricRepository.save(metric);
        
        // Check if any alerts should be triggered
        alertService.checkAlert(savedMetric);
        
        return savedMetric;
    }
    
    public List<Metric> saveMetrics(List<Metric> metrics) {
        List<Metric> savedMetrics = metricRepository.saveAll(metrics);
        
        // Check if any alerts should be triggered for each metric
        for (Metric metric : savedMetrics) {
            alertService.checkAlert(metric);
        }
        
        return savedMetrics;
    }
}
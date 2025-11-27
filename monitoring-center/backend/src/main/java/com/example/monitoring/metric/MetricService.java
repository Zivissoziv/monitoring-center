package com.example.monitoring.metric;

import com.example.monitoring.agent.Agent;
import com.example.monitoring.agent.AgentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetricService {
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private AgentRepository agentRepository;
    
    public List<Metric> getAllMetrics() {
        return metricRepository.findAll();
    }
    
    public List<Metric> getMetricsByAgentId(Long agentId) {
        return metricRepository.findByAgentIdOrderByTimestampDesc(agentId);
    }
    
    public List<Metric> getMetricsByAgentIdAndType(Long agentId, String metricType) {
        return metricRepository.findByAgentIdAndMetricTypeOrderByTimestampDesc(agentId, metricType);
    }
    
    public Metric saveMetric(Metric metric) {
        // Check if agent exists
        Agent agent = agentRepository.findById(metric.getAgentId())
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + metric.getAgentId()));
        
        return metricRepository.save(metric);
    }
    
    public List<Metric> saveMetrics(List<Metric> metrics) {
        return metricRepository.saveAll(metrics);
    }
}
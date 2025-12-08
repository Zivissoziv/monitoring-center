package com.example.monitoring.metric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {
    List<Metric> findByAgentIdOrderByTimestampDesc(String agentId);
    List<Metric> findByAgentIdAndMetricTypeOrderByTimestampDesc(String agentId, String metricType);
    
    // Add pagination support
    Page<Metric> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<Metric> findByAgentIdOrderByTimestampDesc(String agentId, Pageable pageable);
    Page<Metric> findByAgentIdAndMetricTypeOrderByTimestampDesc(String agentId, String metricType, Pageable pageable);
    
    // Add time range query support
    List<Metric> findByAgentIdAndTimestampBetweenOrderByTimestampDesc(String agentId, Long startTime, Long endTime);
    List<Metric> findByAgentIdAndMetricTypeAndTimestampBetweenOrderByTimestampDesc(String agentId, String metricType, Long startTime, Long endTime);
    Page<Metric> findByAgentIdAndTimestampBetweenOrderByTimestampDesc(String agentId, Long startTime, Long endTime, Pageable pageable);
    Page<Metric> findByAgentIdAndMetricTypeAndTimestampBetweenOrderByTimestampDesc(String agentId, String metricType, Long startTime, Long endTime, Pageable pageable);
    
    // For alert checking
    List<Metric> findByAgentIdAndMetricTypeAndTimestampBetween(String agentId, String metricType, Long startTime, Long endTime);
    List<Metric> findByMetricTypeAndTimestampBetween(String metricType, Long startTime, Long endTime);
}
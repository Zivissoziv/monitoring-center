package com.example.monitoring.repository;

import com.example.monitoring.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {
    List<Metric> findByAgentIdOrderByTimestampDesc(String agentId);
    List<Metric> findByAgentIdAndMetricTypeOrderByTimestampDesc(String agentId, String metricType);
    
    // Add pagination support
    Page<Metric> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<Metric> findByAgentIdOrderByTimestampDesc(String agentId, Pageable pageable);
    Page<Metric> findByAgentIdAndMetricTypeOrderByTimestampDesc(String agentId, String metricType, Pageable pageable);
    
    // Add time range query support (LocalDateTime)
    List<Metric> findByAgentIdAndTimestampBetweenOrderByTimestampDesc(String agentId, LocalDateTime startTime, LocalDateTime endTime);
    List<Metric> findByAgentIdAndMetricTypeAndTimestampBetweenOrderByTimestampDesc(String agentId, String metricType, LocalDateTime startTime, LocalDateTime endTime);
    Page<Metric> findByAgentIdAndTimestampBetweenOrderByTimestampDesc(String agentId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    Page<Metric> findByAgentIdAndMetricTypeAndTimestampBetweenOrderByTimestampDesc(String agentId, String metricType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    // For alert checking
    List<Metric> findByAgentIdAndMetricTypeAndTimestampBetween(String agentId, String metricType, LocalDateTime startTime, LocalDateTime endTime);
    List<Metric> findByMetricTypeAndTimestampBetween(String metricType, LocalDateTime startTime, LocalDateTime endTime);
    
    // Filter by app code
    Page<Metric> findByAppCodeInOrderByTimestampDesc(List<String> appCodes, Pageable pageable);
    Page<Metric> findByAppCodeInAndAgentIdOrderByTimestampDesc(List<String> appCodes, String agentId, Pageable pageable);
}
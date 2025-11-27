package com.example.monitoring.metric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {
    List<Metric> findByAgentIdOrderByTimestampDesc(Long agentId);
    List<Metric> findByAgentIdAndMetricTypeOrderByTimestampDesc(Long agentId, String metricType);
}
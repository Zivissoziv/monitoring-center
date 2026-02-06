package com.example.monitoring.repository;

import com.example.monitoring.entity.MetricDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetricDefinitionRepository extends JpaRepository<MetricDefinition, Long> {
    Optional<MetricDefinition> findByMetricName(String metricName);
    List<MetricDefinition> findByEnabled(boolean enabled);
}

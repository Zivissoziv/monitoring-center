package com.example.monitoring.repository;

import com.example.monitoring.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByEnabled(boolean enabled);
    List<AlertRule> findByEnabledTrue();
    List<AlertRule> findByAgentIdAndEnabledTrue(String agentId);
    List<AlertRule> findByMetricTypeAndEnabledTrue(String metricType);
    List<AlertRule> findByMetricTypeAndEnabled(String metricType, boolean enabled);
}
package com.example.monitoring.repository;

import com.example.monitoring.entity.AgentMetricConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentMetricConfigRepository extends JpaRepository<AgentMetricConfig, Long> {
    List<AgentMetricConfig> findByAgentId(String agentId);
    List<AgentMetricConfig> findByAgentIdAndEnabled(String agentId, boolean enabled);
    Optional<AgentMetricConfig> findByAgentIdAndMetricName(String agentId, String metricName);
    List<AgentMetricConfig> findByMetricName(String metricName);
}

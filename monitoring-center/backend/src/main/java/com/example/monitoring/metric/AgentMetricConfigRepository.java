package com.example.monitoring.metric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentMetricConfigRepository extends JpaRepository<AgentMetricConfig, Long> {
    List<AgentMetricConfig> findByAgentId(Long agentId);
    List<AgentMetricConfig> findByAgentIdAndEnabled(Long agentId, boolean enabled);
    Optional<AgentMetricConfig> findByAgentIdAndMetricName(Long agentId, String metricName);
    List<AgentMetricConfig> findByMetricName(String metricName);
}

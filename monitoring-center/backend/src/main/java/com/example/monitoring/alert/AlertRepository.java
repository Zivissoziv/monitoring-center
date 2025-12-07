package com.example.monitoring.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    List<Alert> findByStatus(String status);
    
    List<Alert> findByStatusOrderByLastTriggeredAtDesc(String status);
    
    Optional<Alert> findByAlertRuleIdAndAgentId(Long alertRuleId, Long agentId);
    
    List<Alert> findByAgentId(Long agentId);
    
    List<Alert> findBySeverity(String severity);
}
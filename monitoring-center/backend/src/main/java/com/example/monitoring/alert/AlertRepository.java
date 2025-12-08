package com.example.monitoring.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    List<Alert> findByStatus(String status);
    
    List<Alert> findByStatusOrderByLastTriggeredAtDesc(String status);
    
    Optional<Alert> findByAlertRuleIdAndAgentId(Long alertRuleId, String agentId);
    
    List<Alert> findByAgentId(String agentId);
    
    List<Alert> findBySeverity(String severity);
    
    // Find all active and acknowledged alerts that need to be checked for auto-closure
    List<Alert> findByStatusIn(List<String> statuses);
}
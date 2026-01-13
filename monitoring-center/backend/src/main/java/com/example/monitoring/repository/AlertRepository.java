package com.example.monitoring.repository;

import com.example.monitoring.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    List<Alert> findByStatus(String status);
    
    List<Alert> findByStatusOrderByLastTriggeredAtDesc(String status);
    
    // Find active or acknowledged alert for a specific rule and agent
    Optional<Alert> findByAlertRuleIdAndAgentIdAndStatusIn(Long alertRuleId, String agentId, List<String> statuses);
    
    List<Alert> findByAgentId(String agentId);
    
    List<Alert> findBySeverity(String severity);
    
    // Find all active and acknowledged alerts that need to be checked for auto-closure
    List<Alert> findByStatusIn(List<String> statuses);
    
    // Find alert by external alert ID (for third-party alerts)
    Optional<Alert> findByExternalAlertId(String externalAlertId);
}
package com.example.monitoring.repository;

import com.example.monitoring.entity.Alert;
import com.example.monitoring.enums.AlertStatus;
import com.example.monitoring.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    List<Alert> findByStatus(AlertStatus status);
    
    List<Alert> findByStatusOrderByLastTriggeredAtDesc(AlertStatus status);
    
    // Find active or acknowledged alert for a specific rule and agent
    Optional<Alert> findByAlertRuleIdAndAgentIdAndStatusIn(Long alertRuleId, String agentId, List<AlertStatus> statuses);
    
    List<Alert> findByAgentId(String agentId);
    
    List<Alert> findBySeverity(Severity severity);
    
    // Find all active and acknowledged alerts that need to be checked for auto-closure
    List<Alert> findByStatusIn(List<AlertStatus> statuses);
    
    // Find alert by external alert ID (for third-party alerts)
    Optional<Alert> findByExternalAlertId(String externalAlertId);
    
    // Filter by app code
    List<Alert> findByAppCodeIn(List<String> appCodes);
    
    List<Alert> findByAppCodeInAndStatusOrderByLastTriggeredAtDesc(List<String> appCodes, AlertStatus status);
    
    List<Alert> findByAppCodeInAndStatusInOrderByLastTriggeredAtDesc(List<String> appCodes, List<AlertStatus> statuses);
}
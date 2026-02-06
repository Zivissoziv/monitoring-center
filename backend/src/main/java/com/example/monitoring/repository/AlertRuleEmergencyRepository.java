package com.example.monitoring.repository;

import com.example.monitoring.entity.AlertRuleEmergency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleEmergencyRepository extends JpaRepository<AlertRuleEmergency, Long> {
    List<AlertRuleEmergency> findByAlertRuleId(Long alertRuleId);
    List<AlertRuleEmergency> findByEmergencyKnowledgeId(Long emergencyKnowledgeId);
    void deleteByAlertRuleId(Long alertRuleId);
    void deleteByEmergencyKnowledgeId(Long emergencyKnowledgeId);
    void deleteByAlertRuleIdAndEmergencyKnowledgeId(Long alertRuleId, Long emergencyKnowledgeId);
}

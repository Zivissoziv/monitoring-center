package com.example.monitoring.repository;

import com.example.monitoring.entity.EmergencyStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyStepRepository extends JpaRepository<EmergencyStep, Long> {
    List<EmergencyStep> findByKnowledgeIdOrderByStepOrderAsc(Long knowledgeId);
    void deleteByKnowledgeId(Long knowledgeId);
}

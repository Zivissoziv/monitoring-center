package com.example.monitoring.service;

import com.example.monitoring.entity.AlertRuleEmergency;
import com.example.monitoring.entity.EmergencyKnowledge;
import com.example.monitoring.entity.EmergencyStep;
import com.example.monitoring.repository.AlertRuleEmergencyRepository;
import com.example.monitoring.repository.EmergencyKnowledgeRepository;
import com.example.monitoring.repository.EmergencyStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmergencyKnowledgeService {

    @Autowired
    private EmergencyKnowledgeRepository knowledgeRepository;

    @Autowired
    private EmergencyStepRepository stepRepository;
    
    @Autowired
    private AlertRuleEmergencyRepository alertRuleEmergencyRepository;

    public List<EmergencyKnowledge> getAllKnowledge() {
        List<EmergencyKnowledge> knowledgeList = knowledgeRepository.findAll();
        // Load steps and alert rule associations for each knowledge
        for (EmergencyKnowledge knowledge : knowledgeList) {
            List<EmergencyStep> steps = stepRepository.findByKnowledgeIdOrderByStepOrderAsc(knowledge.getId());
            knowledge.setSteps(steps);
            
            // Load associated alert rule IDs
            List<AlertRuleEmergency> associations = alertRuleEmergencyRepository.findByEmergencyKnowledgeId(knowledge.getId());
            List<Long> alertRuleIds = associations.stream()
                    .map(AlertRuleEmergency::getAlertRuleId)
                    .collect(Collectors.toList());
            knowledge.setAlertRuleIds(alertRuleIds);
        }
        return knowledgeList;
    }
    
    public List<EmergencyKnowledge> getKnowledgeByAlertRuleId(Long alertRuleId) {
        List<AlertRuleEmergency> associations = alertRuleEmergencyRepository.findByAlertRuleId(alertRuleId);
        List<EmergencyKnowledge> knowledgeList = new ArrayList<>();
        
        for (AlertRuleEmergency association : associations) {
            Optional<EmergencyKnowledge> knowledgeOpt = knowledgeRepository.findById(association.getEmergencyKnowledgeId());
            if (knowledgeOpt.isPresent()) {
                EmergencyKnowledge knowledge = knowledgeOpt.get();
                List<EmergencyStep> steps = stepRepository.findByKnowledgeIdOrderByStepOrderAsc(knowledge.getId());
                knowledge.setSteps(steps);
                
                // Load all associated alert rule IDs
                List<AlertRuleEmergency> allAssociations = alertRuleEmergencyRepository.findByEmergencyKnowledgeId(knowledge.getId());
                List<Long> alertRuleIds = allAssociations.stream()
                        .map(AlertRuleEmergency::getAlertRuleId)
                        .collect(Collectors.toList());
                knowledge.setAlertRuleIds(alertRuleIds);
                
                knowledgeList.add(knowledge);
            }
        }
        return knowledgeList;
    }

    @Transactional
    public EmergencyKnowledge createOrUpdateKnowledge(EmergencyKnowledge knowledge) {
        EmergencyKnowledge savedKnowledge;
        
        if (knowledge.getId() != null) {
            // Update existing
            Optional<EmergencyKnowledge> existingOpt = knowledgeRepository.findById(knowledge.getId());
            if (existingOpt.isPresent()) {
                EmergencyKnowledge existing = existingOpt.get();
                existing.setTitle(knowledge.getTitle());
                existing.setDescription(knowledge.getDescription());
                existing.setUpdatedAt(LocalDateTime.now());
                savedKnowledge = knowledgeRepository.save(existing);
                
                // Delete old steps
                stepRepository.deleteByKnowledgeId(savedKnowledge.getId());
            } else {
                savedKnowledge = knowledgeRepository.save(knowledge);
            }
        } else {
            // Create new
            knowledge.setCreatedAt(LocalDateTime.now());
            knowledge.setUpdatedAt(LocalDateTime.now());
            savedKnowledge = knowledgeRepository.save(knowledge);
        }
        
        // Save steps
        if (knowledge.getSteps() != null && !knowledge.getSteps().isEmpty()) {
            for (EmergencyStep step : knowledge.getSteps()) {
                step.setKnowledgeId(savedKnowledge.getId());
                stepRepository.save(step);
            }
            savedKnowledge.setSteps(knowledge.getSteps());
        }
        
        // Update alert rule associations
        if (knowledge.getAlertRuleIds() != null) {
            // Delete old associations
            alertRuleEmergencyRepository.deleteByEmergencyKnowledgeId(savedKnowledge.getId());
            
            // Create new associations
            for (Long alertRuleId : knowledge.getAlertRuleIds()) {
                AlertRuleEmergency association = new AlertRuleEmergency(alertRuleId, savedKnowledge.getId());
                alertRuleEmergencyRepository.save(association);
            }
            savedKnowledge.setAlertRuleIds(knowledge.getAlertRuleIds());
        }
        
        return savedKnowledge;
    }

    @Transactional
    public void deleteKnowledge(Long id) {
        // Delete all steps first
        stepRepository.deleteByKnowledgeId(id);
        // Delete associations
        alertRuleEmergencyRepository.deleteByEmergencyKnowledgeId(id);
        // Delete knowledge
        knowledgeRepository.deleteById(id);
    }
}

package com.example.monitoring.emergency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmergencyKnowledgeService {

    @Autowired
    private EmergencyKnowledgeRepository knowledgeRepository;

    @Autowired
    private EmergencyStepRepository stepRepository;

    public List<EmergencyKnowledge> getAllKnowledge() {
        List<EmergencyKnowledge> knowledgeList = knowledgeRepository.findAll();
        // Load steps for each knowledge
        for (EmergencyKnowledge knowledge : knowledgeList) {
            List<EmergencyStep> steps = stepRepository.findByKnowledgeIdOrderByStepOrderAsc(knowledge.getId());
            knowledge.setSteps(steps);
        }
        return knowledgeList;
    }

    public Optional<EmergencyKnowledge> getKnowledgeByAlertRuleId(Long alertRuleId) {
        Optional<EmergencyKnowledge> knowledgeOpt = knowledgeRepository.findByAlertRuleId(alertRuleId);
        if (knowledgeOpt.isPresent()) {
            EmergencyKnowledge knowledge = knowledgeOpt.get();
            List<EmergencyStep> steps = stepRepository.findByKnowledgeIdOrderByStepOrderAsc(knowledge.getId());
            knowledge.setSteps(steps);
        }
        return knowledgeOpt;
    }

    @Transactional
    public EmergencyKnowledge createOrUpdateKnowledge(EmergencyKnowledge knowledge) {
        // Check if knowledge already exists for this alert rule
        Optional<EmergencyKnowledge> existingOpt = knowledgeRepository.findByAlertRuleId(knowledge.getAlertRuleId());
        
        EmergencyKnowledge savedKnowledge;
        if (existingOpt.isPresent()) {
            // Update existing
            EmergencyKnowledge existing = existingOpt.get();
            existing.setTitle(knowledge.getTitle());
            existing.setDescription(knowledge.getDescription());
            existing.setUpdatedAt(System.currentTimeMillis());
            savedKnowledge = knowledgeRepository.save(existing);
            
            // Delete old steps
            stepRepository.deleteByKnowledgeId(savedKnowledge.getId());
        } else {
            // Create new
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
        
        return savedKnowledge;
    }

    @Transactional
    public void deleteKnowledge(Long id) {
        // Delete all steps first
        stepRepository.deleteByKnowledgeId(id);
        // Delete knowledge
        knowledgeRepository.deleteById(id);
    }

    @Transactional
    public void deleteKnowledgeByAlertRuleId(Long alertRuleId) {
        Optional<EmergencyKnowledge> knowledgeOpt = knowledgeRepository.findByAlertRuleId(alertRuleId);
        if (knowledgeOpt.isPresent()) {
            deleteKnowledge(knowledgeOpt.get().getId());
        }
    }
}

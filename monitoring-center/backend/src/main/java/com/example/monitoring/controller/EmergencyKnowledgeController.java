package com.example.monitoring.controller;

import com.example.monitoring.entity.EmergencyKnowledge;
import com.example.monitoring.service.EmergencyKnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = "*")
public class EmergencyKnowledgeController {

    @Autowired
    private EmergencyKnowledgeService knowledgeService;

    @GetMapping
    public List<EmergencyKnowledge> getAllKnowledge() {
        return knowledgeService.getAllKnowledge();
    }

    @GetMapping("/alert-rule/{alertRuleId}")
    public List<EmergencyKnowledge> getKnowledgeByAlertRuleId(@PathVariable Long alertRuleId) {
        return knowledgeService.getKnowledgeByAlertRuleId(alertRuleId);
    }

    @PostMapping
    public EmergencyKnowledge createOrUpdateKnowledge(@RequestBody EmergencyKnowledge knowledge) {
        return knowledgeService.createOrUpdateKnowledge(knowledge);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ResponseEntity.noContent().build();
    }
}

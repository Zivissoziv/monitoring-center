package com.example.monitoring.emergency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
    public ResponseEntity<EmergencyKnowledge> getKnowledgeByAlertRuleId(@PathVariable Long alertRuleId) {
        Optional<EmergencyKnowledge> knowledge = knowledgeService.getKnowledgeByAlertRuleId(alertRuleId);
        return knowledge.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
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

    @DeleteMapping("/alert-rule/{alertRuleId}")
    public ResponseEntity<Void> deleteKnowledgeByAlertRuleId(@PathVariable Long alertRuleId) {
        knowledgeService.deleteKnowledgeByAlertRuleId(alertRuleId);
        return ResponseEntity.noContent().build();
    }
}

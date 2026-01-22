package com.example.monitoring.controller;

import com.example.monitoring.entity.EmergencyKnowledge;
import com.example.monitoring.service.EmergencyKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EmergencyKnowledgeController {

    private final EmergencyKnowledgeService knowledgeService;

    @GetMapping
    public ResponseEntity<List<EmergencyKnowledge>> getAllKnowledge() {
        return ResponseEntity.ok(knowledgeService.getAllKnowledge());
    }

    @GetMapping("/alert-rule/{alertRuleId}")
    public ResponseEntity<List<EmergencyKnowledge>> getKnowledgeByAlertRuleId(@PathVariable Long alertRuleId) {
        return ResponseEntity.ok(knowledgeService.getKnowledgeByAlertRuleId(alertRuleId));
    }

    @PostMapping
    public ResponseEntity<EmergencyKnowledge> createOrUpdateKnowledge(@RequestBody EmergencyKnowledge knowledge) {
        EmergencyKnowledge saved = knowledgeService.createOrUpdateKnowledge(knowledge);
        return ResponseEntity.status(knowledge.getId() == null ? HttpStatus.CREATED : HttpStatus.OK).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ResponseEntity.noContent().build();
    }
}

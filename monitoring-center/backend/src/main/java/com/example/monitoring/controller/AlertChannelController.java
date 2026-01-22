package com.example.monitoring.controller;

import com.example.monitoring.entity.AlertChannel;
import com.example.monitoring.service.ThirdPartyAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警渠道管理接口 (独立资源)
 */
@Slf4j
@RestController
@RequestMapping("/api/alert-channels")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AlertChannelController {
    
    private final ThirdPartyAlertService thirdPartyAlertService;
    
    @GetMapping
    public ResponseEntity<List<AlertChannel>> getAllChannels() {
        return ResponseEntity.ok(thirdPartyAlertService.getAllChannels());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AlertChannel> getChannelById(@PathVariable Long id) {
        return thirdPartyAlertService.getChannelById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<AlertChannel> createChannel(@RequestBody AlertChannel channel) {
        try {
            AlertChannel saved = thirdPartyAlertService.createChannel(channel);
            log.info("Created new alert channel: {}", saved.getChannelCode());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            log.error("Error creating channel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AlertChannel> updateChannel(@PathVariable Long id, @RequestBody AlertChannel channelDetails) {
        try {
            AlertChannel updated = thirdPartyAlertService.updateChannel(id, channelDetails);
            log.info("Updated alert channel: {}", updated.getChannelCode());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating channel: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id) {
        try {
            thirdPartyAlertService.deleteChannel(id);
            log.info("Deleted alert channel with id: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting channel: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}

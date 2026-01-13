package com.example.monitoring.controller;

import com.example.monitoring.dto.ChannelStatistics;
import com.example.monitoring.dto.ThirdPartyAlertRequest;
import com.example.monitoring.entity.AlertChannel;
import com.example.monitoring.entity.ThirdPartyAlert;
import com.example.monitoring.repository.AlertChannelRepository;
import com.example.monitoring.repository.ThirdPartyAlertRepository;
import com.example.monitoring.service.ThirdPartyAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方告警推送接口
 */
@Slf4j
@RestController
@RequestMapping("/api/third-party-alerts")
public class ThirdPartyAlertController {
    
    @Autowired
    private ThirdPartyAlertService thirdPartyAlertService;
    
    @Autowired
    private AlertChannelRepository channelRepository;
    
    @Autowired
    private ThirdPartyAlertRepository thirdPartyAlertRepository;
    
    /**
     * 接收第三方告警推送
     * POST /api/third-party-alerts/push
     */
    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> pushAlert(
            @Valid @RequestBody ThirdPartyAlertRequest request,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ThirdPartyAlert alert = thirdPartyAlertService.receiveAlert(request, httpRequest);
            
            if ("SUCCESS".equals(alert.getPushStatus())) {
                response.put("success", true);
                response.put("message", "告警推送成功");
                response.put("alertId", alert.getId());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", alert.getFailureReason());
                response.put("alertId", alert.getId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error processing third-party alert push: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "处理失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取所有渠道的统计信息
     * GET /api/third-party-alerts/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<ChannelStatistics>> getStatistics() {
        try {
            List<ChannelStatistics> statistics = thirdPartyAlertService.getChannelStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error getting channel statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取指定渠道的告警记录
     * GET /api/third-party-alerts/channel/{channelCode}
     */
    @GetMapping("/channel/{channelCode}")
    public ResponseEntity<List<ThirdPartyAlert>> getChannelAlerts(@PathVariable String channelCode) {
        try {
            List<ThirdPartyAlert> alerts = thirdPartyAlertService.getChannelAlerts(channelCode);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error getting channel alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取所有告警渠道
     * GET /api/third-party-alerts/channels
     */
    @GetMapping("/channels")
    public ResponseEntity<List<AlertChannel>> getAllChannels() {
        try {
            List<AlertChannel> channels = channelRepository.findAll();
            return ResponseEntity.ok(channels);
        } catch (Exception e) {
            log.error("Error getting all channels: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 创建新的告警渠道
     * POST /api/third-party-alerts/channels
     */
    @PostMapping("/channels")
    public ResponseEntity<AlertChannel> createChannel(@RequestBody AlertChannel channel) {
        try {
            if (channelRepository.existsByChannelCode(channel.getChannelCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
            channel.setCreatedTime(System.currentTimeMillis());
            channel.setUpdatedTime(System.currentTimeMillis());
            
            if (channel.getEnabled() == null) {
                channel.setEnabled(true);
            }
            
            AlertChannel saved = channelRepository.save(channel);
            log.info("Created new alert channel: {}", saved.getChannelCode());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error creating channel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 更新告警渠道
     * PUT /api/third-party-alerts/channels/{id}
     */
    @PutMapping("/channels/{id}")
    public ResponseEntity<AlertChannel> updateChannel(
            @PathVariable Long id,
            @RequestBody AlertChannel channelDetails) {
        try {
            AlertChannel channel = channelRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Channel not found"));
            
            channel.setChannelName(channelDetails.getChannelName());
            channel.setDescription(channelDetails.getDescription());
            channel.setEnabled(channelDetails.getEnabled());
            channel.setContactInfo(channelDetails.getContactInfo());
            channel.setUpdatedTime(System.currentTimeMillis());
            
            AlertChannel updated = channelRepository.save(channel);
            log.info("Updated alert channel: {}", updated.getChannelCode());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating channel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 根据外部告警ID获取所有推送记录
     * GET /api/third-party-alerts/external/{externalAlertId}
     */
    @GetMapping("/external/{externalAlertId}")
    public ResponseEntity<List<ThirdPartyAlert>> getExternalAlertRecords(@PathVariable String externalAlertId) {
        try {
            List<ThirdPartyAlert> records = thirdPartyAlertRepository.findByExternalAlertIdOrderByReceivedTimeDesc(externalAlertId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("Error getting external alert records: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 删除告警渠道
     * DELETE /api/third-party-alerts/channels/{id}
     */
    @DeleteMapping("/channels/{id}")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id) {
        try {
            channelRepository.deleteById(id);
            log.info("Deleted alert channel with id: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting channel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

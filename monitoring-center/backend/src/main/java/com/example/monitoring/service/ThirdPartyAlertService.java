package com.example.monitoring.service;

import com.example.monitoring.dto.ChannelStatistics;
import com.example.monitoring.dto.ThirdPartyAlertRequest;
import com.example.monitoring.entity.Alert;
import com.example.monitoring.entity.AlertChannel;
import com.example.monitoring.entity.ThirdPartyAlert;
import com.example.monitoring.enums.AlertStatus;
import com.example.monitoring.enums.PushStatus;
import com.example.monitoring.enums.Severity;
import com.example.monitoring.enums.ThirdPartyAlertStatus;
import com.example.monitoring.repository.AlertChannelRepository;
import com.example.monitoring.repository.AlertRepository;
import com.example.monitoring.repository.ThirdPartyAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdPartyAlertService {
    
    private final AlertChannelRepository channelRepository;
    private final ThirdPartyAlertRepository alertRepository;
    private final AlertRepository systemAlertRepository;
    
    /**
     * 接收第三方告警推送
     */
    @Transactional
    public ThirdPartyAlert receiveAlert(ThirdPartyAlertRequest request, HttpServletRequest httpRequest) {
        LocalDateTime startTime = LocalDateTime.now();
        String sourceIp = getClientIp(httpRequest);
        
        log.info("Received third-party alert from channel: {}, IP: {}", request.getChannelCode(), sourceIp);
        
        ThirdPartyAlert alert = new ThirdPartyAlert();
        alert.setChannelCode(request.getChannelCode());
        alert.setExternalAlertId(request.getExternalAlertId());
        alert.setAlertContent(request.getAlertContent());
        alert.setAlertStatus(parseThirdPartyAlertStatus(request.getAlertStatus()));
        alert.setSeverity(parseSeverity(request.getSeverity()));
        alert.setReceivedTime(startTime);
        alert.setSourceIp(sourceIp);
        alert.setAppCode(request.getAppCode());
        
        try {
            // 验证渠道是否存在且已启用
            AlertChannel channel = channelRepository.findByChannelCode(request.getChannelCode())
                    .orElseThrow(() -> new RuntimeException("渠道编码不存在: " + request.getChannelCode()));
            
            if (!channel.getEnabled()) {
                throw new RuntimeException("渠道已禁用: " + request.getChannelCode());
            }
            
            // 验证告警状态
            if (!"OPEN".equals(request.getAlertStatus()) && !"CLOSED".equals(request.getAlertStatus())) {
                throw new RuntimeException("无效的告警状态: " + request.getAlertStatus());
            }
            
            alert.setPushStatus(PushStatus.SUCCESS);
            alert.setProcessedTime(LocalDateTime.now());
            
            // 根据告警状态处理系统告警
            if ("OPEN".equals(request.getAlertStatus())) {
                // 创建或更新系统告警记录
                createOrUpdateSystemAlert(channel, request, startTime);
            } else if ("CLOSED".equals(request.getAlertStatus())) {
                // 关闭对应的系统告警
                closeSystemAlert(request, startTime);
            }
            
            log.info("Third-party alert processed successfully from channel: {}", request.getChannelCode());
            
        } catch (Exception e) {
            log.error("Failed to process third-party alert from channel: {}, error: {}", 
                    request.getChannelCode(), e.getMessage(), e);
            alert.setPushStatus(PushStatus.FAILED);
            alert.setFailureReason(e.getMessage());
            alert.setProcessedTime(LocalDateTime.now());
        }
        
        return alertRepository.save(alert);
    }
    
    /**
     * 获取所有渠道的统计信息
     */
    public List<ChannelStatistics> getChannelStatistics() {
        List<ChannelStatistics> statistics = new ArrayList<>();
        List<AlertChannel> channels = channelRepository.findAll();
        
        for (AlertChannel channel : channels) {
            long total = alertRepository.countByChannelCode(channel.getChannelCode());
            long success = alertRepository.countSuccessByChannelCode(channel.getChannelCode());
            long failed = alertRepository.countFailedByChannelCode(channel.getChannelCode());
            
            statistics.add(new ChannelStatistics(
                channel.getChannelCode(),
                channel.getChannelName(),
                total,
                success,
                failed,
                channel.getEnabled()
            ));
        }
        
        return statistics;
    }
    
    /**
     * 获取渠道的告警记录
     */
    public List<ThirdPartyAlert> getChannelAlerts(String channelCode) {
        return alertRepository.findByChannelCode(channelCode);
    }
    
    // ==================== AlertChannel CRUD ====================
    
    public List<AlertChannel> getAllChannels() {
        return channelRepository.findAll();
    }
    
    public Optional<AlertChannel> getChannelById(Long id) {
        return channelRepository.findById(id);
    }
    
    public AlertChannel createChannel(AlertChannel channel) {
        if (channelRepository.existsByChannelCode(channel.getChannelCode())) {
            throw new RuntimeException("渠道编码已存在: " + channel.getChannelCode());
        }
        channel.setCreatedTime(LocalDateTime.now());
        channel.setUpdatedTime(LocalDateTime.now());
        if (channel.getEnabled() == null) {
            channel.setEnabled(true);
        }
        return channelRepository.save(channel);
    }
    
    public AlertChannel updateChannel(Long id, AlertChannel channelDetails) {
        AlertChannel channel = channelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Channel not found with id: " + id));
        
        channel.setChannelName(channelDetails.getChannelName());
        channel.setDescription(channelDetails.getDescription());
        channel.setEnabled(channelDetails.getEnabled());
        channel.setContactInfo(channelDetails.getContactInfo());
        channel.setUpdatedTime(LocalDateTime.now());
        
        return channelRepository.save(channel);
    }
    
    public void deleteChannel(Long id) {
        channelRepository.deleteById(id);
    }
    
    public List<ThirdPartyAlert> getAlertsByExternalId(String externalAlertId) {
        return alertRepository.findByExternalAlertIdOrderByReceivedTimeDesc(externalAlertId);
    }
    
    // ==================== Private Helper Methods ====================
    
    private ThirdPartyAlertStatus parseThirdPartyAlertStatus(String status) {
        if ("OPEN".equalsIgnoreCase(status)) {
            return ThirdPartyAlertStatus.OPEN;
        } else if ("CLOSED".equalsIgnoreCase(status)) {
            return ThirdPartyAlertStatus.CLOSED;
        }
        return ThirdPartyAlertStatus.OPEN;
    }
    
    private Severity parseSeverity(String severity) {
        if (severity == null) {
            return Severity.MEDIUM;
        }
        switch (severity.toUpperCase()) {
            case "LOW":
            case "通知":
            case "提醒":
                return Severity.LOW;
            case "MEDIUM":
            case "一般":
                return Severity.MEDIUM;
            case "HIGH":
            case "重要":
                return Severity.HIGH;
            case "CRITICAL":
            case "致命":
                return Severity.CRITICAL;
            default:
                return Severity.MEDIUM;
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * 创建或更新系统告警记录
     */
    private void createOrUpdateSystemAlert(AlertChannel channel, ThirdPartyAlertRequest request, LocalDateTime timestamp) {
        try {
            Alert systemAlert = systemAlertRepository.findByExternalAlertId(request.getExternalAlertId())
                    .orElse(null);
            
            if (systemAlert != null) {
                // 已存在的告警，更新触发信息
                systemAlert.setLastTriggeredAt(timestamp);
                systemAlert.setTriggerCount(systemAlert.getTriggerCount() + 1);
                systemAlert.setTriggerValueText(request.getAlertContent());
                systemAlert.setSeverity(parseSeverity(request.getSeverity()));
                
                // 如果告警已经被关闭，重新激活
                if (systemAlert.getStatus() == AlertStatus.RESOLVED) {
                    systemAlert.setStatus(AlertStatus.ACTIVE);
                    systemAlert.setResolvedAt(null);
                    systemAlert.setResolveNote(null);
                }
                
                log.info("Updated existing system alert for third-party alert, externalId: {}", 
                        request.getExternalAlertId());
            } else {
                // 创建新告警
                systemAlert = new Alert();
                
                systemAlert.setAlertRuleId(0L); // 第三方告警没有规则ID
                systemAlert.setRuleName("[第三方] " + channel.getChannelName());
                systemAlert.setAgentId(request.getChannelCode());
                systemAlert.setMetricType("THIRD_PARTY");
                systemAlert.setTriggerValueText(request.getAlertContent());
                systemAlert.setSeverity(parseSeverity(request.getSeverity()));
                systemAlert.setExternalAlertId(request.getExternalAlertId());
                
                String alertMessage = String.format("来自渠道 [%s] 的第三方告警", channel.getChannelName());
                alertMessage += "\n外部ID: " + request.getExternalAlertId();
                alertMessage += "\n" + request.getAlertContent();
                systemAlert.setAlertMessage(alertMessage);
                systemAlert.setAppCode(request.getAppCode()); // 设置所属应用
                
                systemAlert.setStatus(AlertStatus.ACTIVE);
                systemAlert.setFirstTriggeredAt(timestamp);
                systemAlert.setLastTriggeredAt(timestamp);
                systemAlert.setTriggerCount(1);
                
                log.info("Created new system alert for third-party alert from channel: {}", 
                        channel.getChannelCode());
            }
            
            systemAlertRepository.save(systemAlert);
            
        } catch (Exception e) {
            log.error("Failed to create/update system alert for third-party alert: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 关闭系统告警
     */
    private void closeSystemAlert(ThirdPartyAlertRequest request, LocalDateTime timestamp) {
        try {
            Optional<Alert> alertOpt = systemAlertRepository.findByExternalAlertId(request.getExternalAlertId());
            if (alertOpt.isPresent()) {
                Alert systemAlert = alertOpt.get();
                
                if (systemAlert.getStatus() == AlertStatus.ACTIVE || 
                    systemAlert.getStatus() == AlertStatus.ACKNOWLEDGED) {
                    systemAlert.setStatus(AlertStatus.RESOLVED);
                    systemAlert.setResolvedAt(timestamp);
                    systemAlert.setResolveNote("第三方告警已关闭: " + request.getAlertContent());
                    
                    systemAlertRepository.save(systemAlert);
                    log.info("Closed system alert for external alert ID: {}", request.getExternalAlertId());
                } else {
                    log.info("Alert already in status: {}, no need to close", systemAlert.getStatus());
                }
            } else {
                log.warn("No system alert found for external alert ID: {}", request.getExternalAlertId());
            }
            
        } catch (Exception e) {
            log.error("Failed to close system alert: {}", e.getMessage(), e);
        }
    }
}

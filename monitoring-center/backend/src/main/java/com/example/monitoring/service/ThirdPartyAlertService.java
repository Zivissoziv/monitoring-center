package com.example.monitoring.service;

import com.example.monitoring.dto.ChannelStatistics;
import com.example.monitoring.dto.ThirdPartyAlertRequest;
import com.example.monitoring.entity.Alert;
import com.example.monitoring.entity.AlertChannel;
import com.example.monitoring.entity.ThirdPartyAlert;
import com.example.monitoring.repository.AlertChannelRepository;
import com.example.monitoring.repository.AlertRepository;
import com.example.monitoring.repository.ThirdPartyAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ThirdPartyAlertService {
    
    @Autowired
    private AlertChannelRepository channelRepository;
    
    @Autowired
    private ThirdPartyAlertRepository alertRepository;
    
    @Autowired
    private AlertRepository systemAlertRepository;
    
    /**
     * 接收第三方告警推送
     */
    @Transactional
    public ThirdPartyAlert receiveAlert(ThirdPartyAlertRequest request, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        String sourceIp = getClientIp(httpRequest);
        
        log.info("Received third-party alert from channel: {}, IP: {}", request.getChannelCode(), sourceIp);
        
        ThirdPartyAlert alert = new ThirdPartyAlert();
        alert.setChannelCode(request.getChannelCode());
        alert.setExternalAlertId(request.getExternalAlertId());
        alert.setAlertContent(request.getAlertContent());
        alert.setAlertStatus(request.getAlertStatus());
        alert.setSeverity(request.getSeverity() != null ? request.getSeverity() : "提醒");
        alert.setReceivedTime(startTime);
        alert.setSourceIp(sourceIp);
        
        try {
            // 验证渠道是否存在且已启用
            AlertChannel channel = channelRepository.findByChannelCode(request.getChannelCode())
                    .orElseThrow(() -> new RuntimeException("渠道编码不存在: " + request.getChannelCode()));
            
            if (!channel.getEnabled()) {
                throw new RuntimeException("渠道已禁用: " + request.getChannelCode());
            }
            
            // 验证告警状态
            if (!request.getAlertStatus().equals("OPEN") && !request.getAlertStatus().equals("CLOSED")) {
                throw new RuntimeException("无效的告警状态: " + request.getAlertStatus());
            }
            
            // 验证告警等级（如果提供）
            if (request.getSeverity() != null) {
                if (!"通知".equals(request.getSeverity()) &&
                    !"提醒".equals(request.getSeverity()) &&
                    !"一般".equals(request.getSeverity()) &&
                    !"重要".equals(request.getSeverity()) &&
                    !"致命".equals(request.getSeverity())) {
                    throw new RuntimeException("无效的告警等级: " + request.getSeverity());
                }
            }
            
            alert.setPushStatus("SUCCESS");
            alert.setProcessedTime(System.currentTimeMillis());
            
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
            alert.setPushStatus("FAILED");
            alert.setFailureReason(e.getMessage());
            alert.setProcessedTime(System.currentTimeMillis());
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
    
    /**
     * 获取客户端IP地址
     */
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
    private void createOrUpdateSystemAlert(AlertChannel channel, ThirdPartyAlertRequest request, long timestamp) {
        try {
            Alert systemAlert = null;
            
            // 根据外部ID查找是否已存在
            systemAlert = systemAlertRepository.findByExternalAlertId(request.getExternalAlertId())
                    .orElse(null);
            
            if (systemAlert != null) {
                // 已存在的告警，更新触发信息
                systemAlert.setLastTriggeredAt(timestamp);
                systemAlert.setTriggerCount(systemAlert.getTriggerCount() + 1);
                systemAlert.setTriggerValueText(request.getAlertContent());
                systemAlert.setSeverity(request.getSeverity() != null ? request.getSeverity() : "提醒");
                
                // 如果告警已经被关闭，重新激活
                if ("RESOLVED".equals(systemAlert.getStatus())) {
                    systemAlert.setStatus("ACTIVE");
                    systemAlert.setResolvedAt(null);
                    systemAlert.setResolveNote(null);
                }
                
                log.info("Updated existing system alert for third-party alert, externalId: {}", 
                        request.getExternalAlertId());
            } else {
                // 创建新告警
                systemAlert = new Alert();
                
                // 设置告警规则信息（使用渠道信息作为规则）
                systemAlert.setAlertRuleId(0L); // 第三方告警没有规则ID
                systemAlert.setRuleName("[第三方] " + channel.getChannelName());
                
                // 代理信息使用渠道编码
                systemAlert.setAgentId(request.getChannelCode());
                systemAlert.setMetricType("THIRD_PARTY");
                
                // 告警内容
                systemAlert.setTriggerValueText(request.getAlertContent());
                systemAlert.setSeverity(request.getSeverity() != null ? request.getSeverity() : "提醒");
                
                // 外部ID
                systemAlert.setExternalAlertId(request.getExternalAlertId());
                
                // 自定义告警消息
                String alertMessage = String.format("来自渠道 [%s] 的第三方告警", channel.getChannelName());
                alertMessage += "\n外部ID: " + request.getExternalAlertId();
                alertMessage += "\n" + request.getAlertContent();
                systemAlert.setAlertMessage(alertMessage);
                
                // 设置状态和时间
                systemAlert.setStatus("ACTIVE");
                systemAlert.setFirstTriggeredAt(timestamp);
                systemAlert.setLastTriggeredAt(timestamp);
                systemAlert.setTriggerCount(1);
                
                log.info("Created new system alert for third-party alert from channel: {}", 
                        channel.getChannelCode());
            }
            
            systemAlertRepository.save(systemAlert);
            
        } catch (Exception e) {
            log.error("Failed to create/update system alert for third-party alert: {}", e.getMessage(), e);
            // 不抛出异常，避免影响第三方告警记录的保存
        }
    }
    
    /**
     * 关闭系统告警
     */
    private void closeSystemAlert(ThirdPartyAlertRequest request, long timestamp) {
        try {
            Optional<Alert> alertOpt = systemAlertRepository.findByExternalAlertId(request.getExternalAlertId());
            if (alertOpt.isPresent()) {
                Alert systemAlert = alertOpt.get();
                
                // 只有活动状态的告警才需要关闭
                if ("ACTIVE".equals(systemAlert.getStatus()) || "ACKNOWLEDGED".equals(systemAlert.getStatus())) {
                    systemAlert.setStatus("RESOLVED");
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
            // 不抛出异常，避免影响第三方告警记录的保存
        }
    }
}

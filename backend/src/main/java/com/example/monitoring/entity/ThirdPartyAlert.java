package com.example.monitoring.entity;

import com.example.monitoring.enums.PushStatus;
import com.example.monitoring.enums.Severity;
import com.example.monitoring.enums.ThirdPartyAlertStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 第三方告警推送记录
 */
@Entity
@Table(name = "third_party_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "channel_code", nullable = false, length = 50)
    private String channelCode; // 渠道编码
    
    @Column(name = "external_alert_id", length = 100)
    private String externalAlertId; // 第三方告警主键
    
    @Column(name = "alert_content", nullable = false, columnDefinition = "TEXT")
    private String alertContent; // 告警内容
    
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_status", nullable = false, length = 20)
    private ThirdPartyAlertStatus alertStatus; // 告警状态：OPEN, CLOSED
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private Severity severity; // 告警等级：CRITICAL, WARNING, INFO
    
    @Enumerated(EnumType.STRING)
    @Column(name = "push_status", nullable = false, length = 20)
    private PushStatus pushStatus; // 推送状态：SUCCESS, FAILED
    
    @Column(name = "failure_reason", length = 500)
    private String failureReason; // 失败原因
    
    @Column(name = "received_time", nullable = false)
    private LocalDateTime receivedTime; // 接收时间
    
    @Column(name = "processed_time")
    private LocalDateTime processedTime; // 处理时间
    
    @Column(name = "source_ip", length = 50)
    private String sourceIp; // 来源IP
    
    @Column(name = "app_code", nullable = false, length = 10)
    private String appCode; // 所属应用
}

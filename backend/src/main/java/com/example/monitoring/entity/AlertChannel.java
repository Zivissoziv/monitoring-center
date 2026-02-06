package com.example.monitoring.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 第三方告警渠道配置
 */
@Entity
@Table(name = "alert_channels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "channel_code", unique = true, nullable = false, length = 50)
    private String channelCode; // 渠道编码，唯一标识
    
    @Column(name = "channel_name", nullable = false, length = 100)
    private String channelName; // 渠道名称
    
    @Column(name = "description", length = 500)
    private String description; // 渠道描述
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true; // 是否启用
    
    @Column(name = "created_time")
    private LocalDateTime createdTime; // 创建时间
    
    @Column(name = "updated_time")
    private LocalDateTime updatedTime; // 更新时间
    
    @Column(name = "contact_info", length = 200)
    private String contactInfo; // 联系方式
}

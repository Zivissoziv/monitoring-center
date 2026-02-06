package com.example.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelStatistics {
    private String channelCode;
    private String channelName;
    private Long totalAlerts;
    private Long successCount;
    private Long failedCount;
    private Boolean enabled;
}

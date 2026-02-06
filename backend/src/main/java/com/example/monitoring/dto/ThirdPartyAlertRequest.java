package com.example.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 第三方告警推送请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyAlertRequest {
    
    @NotBlank(message = "渠道编码不能为空")
    private String channelCode; // 渠道编码
    
    @NotBlank(message = "外部告警ID不能为空")
    private String externalAlertId; // 第三方告警主键（必填）
    
    @NotBlank(message = "告警内容不能为空")
    private String alertContent; // 告警内容
    
    @NotNull(message = "告警状态不能为空")
    private String alertStatus; // 告警状态：OPEN, CLOSED
    
    private String severity; // 告警等级：CRITICAL, WARNING, INFO（可选，默认WARNING）
    
    @NotBlank(message = "所属应用不能为空")
    private String appCode; // 所属应用（必填）
}

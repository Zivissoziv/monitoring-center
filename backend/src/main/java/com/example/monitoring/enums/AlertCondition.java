package com.example.monitoring.enums;

/**
 * 告警规则条件枚举
 */
public enum AlertCondition {
    // 数值比较
    GT,         // 大于
    LT,         // 小于
    EQ,         // 等于
    GTE,        // 大于等于
    LTE,        // 小于等于
    
    // 字符串/布尔比较
    EQUALS,     // 字符串相等
    NOT_EQUALS, // 字符串不相等
    CONTAINS    // 字符串包含
}

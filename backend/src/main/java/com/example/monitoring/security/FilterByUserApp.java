package com.example.monitoring.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要根据用户应用权限过滤返回数据的接口
 * 支持过滤包含 appCode 字段的实体列表
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterByUserApp {
}

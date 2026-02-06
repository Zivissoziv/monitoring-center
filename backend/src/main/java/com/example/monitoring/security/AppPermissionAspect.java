package com.example.monitoring.security;

import com.example.monitoring.entity.Alert;
import com.example.monitoring.entity.Metric;
import com.example.monitoring.entity.SysUser;
import com.example.monitoring.repository.SysUserRepository;
import com.example.monitoring.service.AppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AOP切面：根据用户应用权限过滤返回数据
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AppPermissionAspect {

    private final AppService appService;
    private final SysUserRepository userRepository;

    /**
     * 拦截标注了 @FilterByUserApp 的方法，过滤返回数据
     */
    @Around("@annotation(com.example.monitoring.security.FilterByUserApp)")
    public Object filterByUserApp(ProceedingJoinPoint joinPoint) throws Throwable {
        // 先执行原方法
        Object result = joinPoint.proceed();
        
        // 获取当前用户可访问的应用列表
        List<String> accessibleAppCodes = getAccessibleAppCodes();
        
        // 如果获取不到用户信息，返回空数据
        if (accessibleAppCodes == null) {
            return createEmptyResponse(result);
        }
        
        // 过滤返回数据
        return filterResult(result, accessibleAppCodes);
    }

    /**
     * 获取当前用户可访问的应用列表
     */
    private List<String> getAccessibleAppCodes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        
        Long userId = userRepository.findByUsername(auth.getName())
                .map(SysUser::getId)
                .orElse(null);
        
        if (userId == null) {
            return null;
        }
        
        return appService.getAccessibleAppCodes(userId);
    }

    /**
     * 根据应用权限过滤返回结果
     */
    @SuppressWarnings("unchecked")
    private Object filterResult(Object result, List<String> accessibleAppCodes) {
        if (result == null) {
            return null;
        }

        // 处理 ResponseEntity 包装
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            Object body = responseEntity.getBody();
            Object filteredBody = filterData(body, accessibleAppCodes);
            return ResponseEntity.status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(filteredBody);
        }

        // 直接返回的数据
        return filterData(result, accessibleAppCodes);
    }

    /**
     * 过滤数据
     */
    @SuppressWarnings("unchecked")
    private Object filterData(Object data, List<String> accessibleAppCodes) {
        if (data == null) {
            return null;
        }

        // 处理 Page 类型
        if (data instanceof Page) {
            Page<?> page = (Page<?>) data;
            List<?> filteredContent = filterList(page.getContent(), accessibleAppCodes);
            return new PageImpl<>(filteredContent, page.getPageable(), filteredContent.size());
        }

        // 处理 List 类型
        if (data instanceof List) {
            return filterList((List<?>) data, accessibleAppCodes);
        }

        // 单个对象
        if (data instanceof Alert) {
            Alert alert = (Alert) data;
            if (isAccessible(alert.getAppCode(), accessibleAppCodes)) {
                return alert;
            }
            return null;
        }

        if (data instanceof Metric) {
            Metric metric = (Metric) data;
            if (isAccessible(metric.getAppCode(), accessibleAppCodes)) {
                return metric;
            }
            return null;
        }

        // 其他类型直接返回
        return data;
    }

    /**
     * 过滤列表数据
     */
    private List<?> filterList(List<?> list, List<String> accessibleAppCodes) {
        if (list == null || list.isEmpty()) {
            return list;
        }

        Object first = list.get(0);
        
        // Alert 列表
        if (first instanceof Alert) {
            return list.stream()
                    .map(item -> (Alert) item)
                    .filter(alert -> isAccessible(alert.getAppCode(), accessibleAppCodes))
                    .collect(Collectors.toList());
        }

        // Metric 列表
        if (first instanceof Metric) {
            return list.stream()
                    .map(item -> (Metric) item)
                    .filter(metric -> isAccessible(metric.getAppCode(), accessibleAppCodes))
                    .collect(Collectors.toList());
        }

        // 其他类型直接返回
        return list;
    }

    /**
     * 判断是否有权访问
     */
    private boolean isAccessible(String appCode, List<String> accessibleAppCodes) {
        // 无应用标识的数据，所有人可见
        if (appCode == null || appCode.isEmpty()) {
            return true;
        }
        return accessibleAppCodes.contains(appCode);
    }

    /**
     * 创建空响应
     */
    private Object createEmptyResponse(Object originalResult) {
        if (originalResult instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) originalResult;
            Object body = responseEntity.getBody();
            
            if (body instanceof Page) {
                return ResponseEntity.status(responseEntity.getStatusCode())
                        .headers(responseEntity.getHeaders())
                        .body(Page.empty());
            }
            if (body instanceof List) {
                return ResponseEntity.status(responseEntity.getStatusCode())
                        .headers(responseEntity.getHeaders())
                        .body(Collections.emptyList());
            }
            return ResponseEntity.status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(null);
        }
        
        if (originalResult instanceof Page) {
            return Page.empty();
        }
        if (originalResult instanceof List) {
            return Collections.emptyList();
        }
        return null;
    }
}

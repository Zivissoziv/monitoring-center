package com.example.monitoring.controller;

import com.example.monitoring.dto.ApiResponse;
import com.example.monitoring.entity.SysApp;
import com.example.monitoring.service.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apps")
public class AppController {

    @Autowired
    private AppService appService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SysApp>>> getAllApps() {
        List<SysApp> apps = appService.getAllApps();
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<SysApp>>> getEnabledApps() {
        List<SysApp> apps = appService.getEnabledApps();
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SysApp>> getAppById(@PathVariable Long id) {
        return appService.getAppById(id)
                .map(app -> ResponseEntity.ok(ApiResponse.success(app)))
                .orElse(ResponseEntity.ok(ApiResponse.error("应用不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SysApp>> createApp(@RequestBody SysApp app) {
        try {
            SysApp created = appService.createApp(app);
            return ResponseEntity.ok(ApiResponse.success(created));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SysApp>> updateApp(@PathVariable Long id, @RequestBody SysApp app) {
        try {
            SysApp updated = appService.updateApp(id, app);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApp(@PathVariable Long id) {
        try {
            appService.deleteApp(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // User-App association endpoints
    @GetMapping("/{appCode}/users")
    public ResponseEntity<ApiResponse<List<Long>>> getAppUsers(@PathVariable String appCode) {
        List<Long> userIds = appService.getAppUsers(appCode);
        return ResponseEntity.ok(ApiResponse.success(userIds));
    }

    @PostMapping("/{appCode}/users")
    public ResponseEntity<ApiResponse<Void>> assignUsersToApp(
            @PathVariable String appCode,
            @RequestBody Map<String, List<Long>> request) {
        List<Long> userIds = request.get("userIds");
        if (userIds == null) {
            return ResponseEntity.ok(ApiResponse.error("userIds不能为空"));
        }
        appService.assignUsersToApp(appCode, userIds);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<SysApp>>> getUserApps(@PathVariable Long userId) {
        List<SysApp> apps = appService.getUserApps(userId);
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @GetMapping("/user/{userId}/codes")
    public ResponseEntity<ApiResponse<List<String>>> getUserAppCodes(@PathVariable Long userId) {
        List<String> appCodes = appService.getUserAppCodes(userId);
        return ResponseEntity.ok(ApiResponse.success(appCodes));
    }

    @PostMapping("/user/{userId}/assign")
    public ResponseEntity<ApiResponse<Void>> assignAppsToUser(
            @PathVariable Long userId,
            @RequestBody Map<String, List<String>> request) {
        List<String> appCodes = request.get("appCodes");
        if (appCodes == null) {
            return ResponseEntity.ok(ApiResponse.error("appCodes不能为空"));
        }
        appService.assignAppsToUser(userId, appCodes);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

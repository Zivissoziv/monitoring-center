package com.example.monitoring.service;

import com.example.monitoring.entity.SysApp;
import com.example.monitoring.entity.SysUser;
import com.example.monitoring.entity.SysUserApp;
import com.example.monitoring.repository.SysAppRepository;
import com.example.monitoring.repository.SysUserAppRepository;
import com.example.monitoring.repository.SysUserRepository;
import com.example.monitoring.repository.SysRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppService {

    @Autowired
    private SysAppRepository appRepository;

    @Autowired
    private SysUserAppRepository userAppRepository;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysRoleRepository roleRepository;

    public List<SysApp> getAllApps() {
        return appRepository.findAll();
    }

    public List<SysApp> getEnabledApps() {
        return appRepository.findByEnabled(true);
    }

    public Optional<SysApp> getAppById(Long id) {
        return appRepository.findById(id);
    }

    public Optional<SysApp> getAppByCode(String appCode) {
        return appRepository.findByAppCode(appCode);
    }

    @Transactional
    public SysApp createApp(SysApp app) {
        if (appRepository.existsByAppCode(app.getAppCode())) {
            throw new RuntimeException("应用编码已存在: " + app.getAppCode());
        }
        return appRepository.save(app);
    }

    @Transactional
    public SysApp updateApp(Long id, SysApp appDetails) {
        SysApp app = appRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("应用不存在: " + id));
        
        // Check if new appCode conflicts with existing ones
        if (!app.getAppCode().equals(appDetails.getAppCode()) 
                && appRepository.existsByAppCode(appDetails.getAppCode())) {
            throw new RuntimeException("应用编码已存在: " + appDetails.getAppCode());
        }
        
        app.setAppCode(appDetails.getAppCode());
        app.setAppName(appDetails.getAppName());
        app.setDescription(appDetails.getDescription());
        app.setEnabled(appDetails.getEnabled());
        
        return appRepository.save(app);
    }

    @Transactional
    public void deleteApp(Long id) {
        SysApp app = appRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("应用不存在: " + id));
        userAppRepository.deleteByAppCode(app.getAppCode());
        appRepository.deleteById(id);
    }

    // User-App association methods
    public List<String> getUserAppCodes(Long userId) {
        return userAppRepository.findAppCodesByUserId(userId);
    }

    public List<SysApp> getUserApps(Long userId) {
        List<String> appCodes = userAppRepository.findAppCodesByUserId(userId);
        if (appCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return appRepository.findAll().stream()
                .filter(app -> appCodes.contains(app.getAppCode()))
                .collect(Collectors.toList());
    }

    public List<Long> getAppUsers(String appCode) {
        return userAppRepository.findUserIdsByAppCode(appCode);
    }

    @Transactional
    public void assignAppsToUser(Long userId, List<String> appCodes) {
        userAppRepository.deleteByUserId(userId);
        for (String appCode : appCodes) {
            if (appRepository.existsByAppCode(appCode)) {
                userAppRepository.save(new SysUserApp(userId, appCode));
            }
        }
    }

    @Transactional
    public void assignUsersToApp(String appCode, List<Long> userIds) {
        userAppRepository.deleteByAppCode(appCode);
        for (Long userId : userIds) {
            userAppRepository.save(new SysUserApp(userId, appCode));
        }
    }

    /**
     * Check if user has permission to access app data.
     * Admin role users can access all app data.
     */
    public boolean userHasAppPermission(Long userId, String appCode) {
        if (appCode == null || appCode.isEmpty()) {
            return true; // No app restriction
        }
        
        // Check if user is admin
        SysUser user = userRepository.findById(userId).orElse(null);
        if (user != null && isAdmin(userId)) {
            return true;
        }
        
        return userAppRepository.existsByUserIdAndAppCode(userId, appCode);
    }

    /**
     * Get all app codes that a user can access.
     * Admin users get all enabled app codes.
     */
    public List<String> getAccessibleAppCodes(Long userId) {
        if (isAdmin(userId)) {
            return appRepository.findByEnabled(true).stream()
                    .map(SysApp::getAppCode)
                    .collect(Collectors.toList());
        }
        return userAppRepository.findAppCodesByUserId(userId);
    }

    /**
     * Check if user has admin role
     */
    public boolean isAdmin(Long userId) {
        return roleRepository.findRoleCodesByUserId(userId).contains("admin");
    }
}

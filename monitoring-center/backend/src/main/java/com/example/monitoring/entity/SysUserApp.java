package com.example.monitoring.entity;

import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "sys_user_apps")
public class SysUserApp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "app_code", nullable = false, length = 10)
    private String appCode;

    public SysUserApp() {}

    public SysUserApp(Long userId, String appCode) {
        this.userId = userId;
        this.appCode = appCode;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }
}

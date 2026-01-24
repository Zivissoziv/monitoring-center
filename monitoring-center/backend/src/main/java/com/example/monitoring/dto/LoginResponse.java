package com.example.monitoring.dto;

import com.example.monitoring.entity.SysApp;
import com.example.monitoring.entity.SysMenu;
import com.example.monitoring.entity.SysRole;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private List<SysRole> roles;
    private List<SysMenu> menus;
    private List<SysApp> apps;
}

package com.example.monitoring.service;

import com.example.monitoring.dto.LoginRequest;
import com.example.monitoring.dto.LoginResponse;
import com.example.monitoring.entity.SysApp;
import com.example.monitoring.entity.SysMenu;
import com.example.monitoring.entity.SysRole;
import com.example.monitoring.entity.SysUser;
import com.example.monitoring.repository.SysMenuRepository;
import com.example.monitoring.repository.SysRoleRepository;
import com.example.monitoring.repository.SysUserRepository;
import com.example.monitoring.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysMenuRepository menuRepository;
    private final AppService appService;
    
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        SysUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        String token = jwtUtils.generateToken(user.getUsername(), user.getId());
        List<SysRole> roles = roleRepository.findRolesByUserId(user.getId());
        List<SysMenu> menus = menuRepository.findMenusByUserId(user.getId());
        List<SysApp> apps = appService.getUserApps(user.getId());
        
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roles(roles)
                .menus(menus)
                .apps(apps)
                .build();
    }
    
    public LoginResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        List<SysRole> roles = roleRepository.findRolesByUserId(user.getId());
        List<SysMenu> menus = menuRepository.findMenusByUserId(user.getId());
        List<SysApp> apps = appService.getUserApps(user.getId());
        
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roles(roles)
                .menus(menus)
                .apps(apps)
                .build();
    }
}

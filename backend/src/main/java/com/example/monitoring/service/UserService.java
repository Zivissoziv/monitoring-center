package com.example.monitoring.service;

import com.example.monitoring.dto.UserDTO;
import com.example.monitoring.entity.SysRole;
import com.example.monitoring.entity.SysUser;
import com.example.monitoring.entity.SysUserRole;
import com.example.monitoring.repository.SysRoleRepository;
import com.example.monitoring.repository.SysUserRepository;
import com.example.monitoring.repository.SysUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> getUserById(Long id) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return convertToMap(user);
    }
    
    @Transactional
    public SysUser createUser(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        
        user = userRepository.save(user);
        
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            saveUserRoles(user.getId(), dto.getRoleIds());
        }
        
        return user;
    }
    
    @Transactional
    public SysUser updateUser(Long id, UserDTO dto) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // Check if username is changed and already exists
        if (!user.getUsername().equals(dto.getUsername()) 
                && userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        user.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setNickname(dto.getNickname());
        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }
        
        user = userRepository.save(user);
        
        // Update roles
        if (dto.getRoleIds() != null) {
            userRoleRepository.deleteByUserId(id);
            if (!dto.getRoleIds().isEmpty()) {
                saveUserRoles(id, dto.getRoleIds());
            }
        }
        
        return user;
    }
    
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在");
        }
        userRoleRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }
    
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleRepository.save(userRole);
        }
    }
    
    private Map<String, Object> convertToMap(SysUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("enabled", user.getEnabled());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        
        List<SysRole> roles = roleRepository.findRolesByUserId(user.getId());
        map.put("roles", roles);
        map.put("roleIds", roles.stream().map(SysRole::getId).collect(Collectors.toList()));
        
        return map;
    }
}

package com.example.monitoring.service;

import com.example.monitoring.dto.RoleDTO;
import com.example.monitoring.entity.SysMenu;
import com.example.monitoring.entity.SysRole;
import com.example.monitoring.entity.SysRoleMenu;
import com.example.monitoring.repository.SysMenuRepository;
import com.example.monitoring.repository.SysRoleMenuRepository;
import com.example.monitoring.repository.SysRoleRepository;
import com.example.monitoring.repository.SysUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {
    
    private final SysRoleRepository roleRepository;
    private final SysMenuRepository menuRepository;
    private final SysRoleMenuRepository roleMenuRepository;
    private final SysUserRoleRepository userRoleRepository;
    
    public List<Map<String, Object>> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> getRoleById(Long id) {
        SysRole role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        return convertToMap(role);
    }
    
    @Transactional
    public SysRole createRole(RoleDTO dto) {
        if (roleRepository.existsByRoleCode(dto.getRoleCode())) {
            throw new RuntimeException("角色编码已存在");
        }
        
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        
        role = roleRepository.save(role);
        
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            saveRoleMenus(role.getId(), dto.getMenuIds());
        }
        
        return role;
    }
    
    @Transactional
    public SysRole updateRole(Long id, RoleDTO dto) {
        SysRole role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        // Check if role code is changed and already exists
        if (!role.getRoleCode().equals(dto.getRoleCode()) 
                && roleRepository.existsByRoleCode(dto.getRoleCode())) {
            throw new RuntimeException("角色编码已存在");
        }
        
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        
        role = roleRepository.save(role);
        
        // Update menus
        if (dto.getMenuIds() != null) {
            roleMenuRepository.deleteByRoleId(id);
            if (!dto.getMenuIds().isEmpty()) {
                saveRoleMenus(id, dto.getMenuIds());
            }
        }
        
        return role;
    }
    
    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new RuntimeException("角色不存在");
        }
        userRoleRepository.deleteByRoleId(id);
        roleMenuRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }
    
    public List<SysMenu> getAllMenus() {
        return menuRepository.findByEnabledTrueOrderBySortOrderAsc();
    }
    
    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenuRepository.save(roleMenu);
        }
    }
    
    private Map<String, Object> convertToMap(SysRole role) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", role.getId());
        map.put("roleCode", role.getRoleCode());
        map.put("roleName", role.getRoleName());
        map.put("description", role.getDescription());
        map.put("createdAt", role.getCreatedAt());
        
        List<SysMenu> menus = menuRepository.findMenusByRoleId(role.getId());
        map.put("menus", menus);
        map.put("menuIds", menus.stream().map(SysMenu::getId).collect(Collectors.toList()));
        
        return map;
    }
}

package com.example.monitoring.controller;

import com.example.monitoring.dto.ApiResponse;
import com.example.monitoring.dto.RoleDTO;
import com.example.monitoring.entity.SysMenu;
import com.example.monitoring.entity.SysRole;
import com.example.monitoring.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    
    private final RoleService roleService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllRoles() {
        List<Map<String, Object>> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoleById(@PathVariable Long id) {
        try {
            Map<String, Object> role = roleService.getRoleById(id);
            return ResponseEntity.ok(ApiResponse.success(role));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SysRole>> createRole(@Valid @RequestBody RoleDTO dto) {
        try {
            SysRole role = roleService.createRole(dto);
            return ResponseEntity.ok(ApiResponse.success(role));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SysRole>> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        try {
            SysRole role = roleService.updateRole(id, dto);
            return ResponseEntity.ok(ApiResponse.success(role));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SysMenu>>> getAllMenus() {
        List<SysMenu> menus = roleService.getAllMenus();
        return ResponseEntity.ok(ApiResponse.success(menus));
    }
}

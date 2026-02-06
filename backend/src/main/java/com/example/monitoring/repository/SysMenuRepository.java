package com.example.monitoring.repository;

import com.example.monitoring.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {
    
    List<SysMenu> findByEnabledTrueOrderBySortOrderAsc();
    
    @Query("SELECT DISTINCT m FROM SysMenu m " +
           "JOIN SysRoleMenu rm ON m.id = rm.menuId " +
           "JOIN SysUserRole ur ON rm.roleId = ur.roleId " +
           "WHERE ur.userId = :userId AND m.enabled = true " +
           "ORDER BY m.sortOrder ASC")
    List<SysMenu> findMenusByUserId(@Param("userId") Long userId);
    
    @Query("SELECT m FROM SysMenu m " +
           "JOIN SysRoleMenu rm ON m.id = rm.menuId " +
           "WHERE rm.roleId = :roleId AND m.enabled = true " +
           "ORDER BY m.sortOrder ASC")
    List<SysMenu> findMenusByRoleId(@Param("roleId") Long roleId);
}

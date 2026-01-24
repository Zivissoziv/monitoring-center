package com.example.monitoring.repository;

import com.example.monitoring.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {
    
    List<SysUserRole> findByUserId(Long userId);
    
    List<SysUserRole> findByRoleId(Long roleId);
    
    @Modifying
    @Query("DELETE FROM SysUserRole ur WHERE ur.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM SysUserRole ur WHERE ur.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}

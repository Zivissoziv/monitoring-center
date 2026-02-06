package com.example.monitoring.repository;

import com.example.monitoring.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {
    
    Optional<SysRole> findByRoleCode(String roleCode);
    
    boolean existsByRoleCode(String roleCode);
    
    @Query("SELECT r FROM SysRole r JOIN SysUserRole ur ON r.id = ur.roleId WHERE ur.userId = :userId")
    List<SysRole> findRolesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT r.roleCode FROM SysRole r JOIN SysUserRole ur ON r.id = ur.roleId WHERE ur.userId = :userId")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
}
